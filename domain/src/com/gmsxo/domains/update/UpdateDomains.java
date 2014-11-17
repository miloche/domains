package com.gmsxo.domains.update;

import static com.gmsxo.domains.config.AppConfig.CFG_EXT_RES;
import static com.gmsxo.domains.config.AppConfig.CFG_EXPORT_SUB_DIR;
import static com.gmsxo.domains.config.AppConfig.CFG_BACKUP_SUB_DIR;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.gmsxo.domains.config.ConfigLoader;
import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.DNSHelper;
import com.gmsxo.domains.helpers.FileHelper;
import com.gmsxo.domains.helpers.ImportHelper;
import com.gmsxo.domains.resolve.v1.NSLookupThread;
import com.gmsxo.domains.resolve.v1.NSLookupThread.NSLookupResult;

/**
 * Update the domain tables from an input file.
 * 
 * @author miloxe
 *
 */
public class UpdateDomains implements Runnable {
  private static final Logger LOG=Logger.getLogger(UpdateDomains.class);
  private static final long CONST1000=1000L;
  private static final String WORKING_DIR_KEY_NAME="WORKINGDIR";
  private static final String LOOKUP_TIMEOUT_KEY_NAME="LOOKUPTIMEOUT";
  private static final String SLEEP_TIME_KEY_NAME="SLEEPTIME";
  private static final String SUSPEND_KEY_NAME="SLEEPTIME";
  
  private String workingDir;
  private int lookupTimeout;
  private long sleepTime;
  private boolean suspend;
  
  private IpAddress nullIp;
  private Session ses;
  private boolean isErrorPass=false;
  private String domainFileName;
  private int numberOfAll;
  private int errorsResolved;

  private ExecutorService executor;
  private Map<Domain,Domain> errorDomains=new TreeMap<>();
  private Set<Domain> domainsToUpdate=new TreeSet<>();
  private Map<String,IpAddress> newIpAddresses=new TreeMap<>();
  private Map<String,DnsServer>  newDnsServers=new TreeMap<>();
  private List<Future<NSLookupResult>>resultList=new ArrayList<>();

  /**
   * The main loop. Reads configuration and executes the task.
   * 
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    while (true) {
      loadConfig();
      try { doJob(workingDir); } catch (IOException e) { LOG.error("IO",e); }
      if (!suspend) try { Thread.sleep(sleepTime); } catch (InterruptedException e) { LOG.error("interrupted",e); }
    }
  }
 /**
   * The executive method which can be called from run or main. It goes through the working folder and process the input files.
   * Reads all domains from input files and updates the database accordingly.
   *  
   * @param workingDir
   * @throws IOException
   */
  public void doJob(final String workingDir) throws IOException {
    FileHelper.checkDirs(workingDir);
    try {
      for (String letter:ImportHelper.pref) {
        String actualDir = workingDir+CFG_EXPORT_SUB_DIR+letter+File.separator;
        String backupDir = workingDir+CFG_BACKUP_SUB_DIR+letter+File.separator;
        next_file: while (true) {
          long start=new Date().getTime();
          if (!readDomains(actualDir)) break next_file;
          checkErrors();
          updateDomains();
          moveFile(actualDir, backupDir);
          logResults(start);
          cleanUp();
        } // end next_file
      } //end next letter
    }
    finally {if (ses!=null && ses.isOpen()) ses.close();}
  }  
  /**
   * Loads the configuration from database.
   */
  private void loadConfig() {
    try {
      ConfigLoader cfl=ConfigLoader.loadConfig(getClass().getName());
      workingDir=cfl.getString(WORKING_DIR_KEY_NAME);
      lookupTimeout=cfl.getInteger(LOOKUP_TIMEOUT_KEY_NAME);
      sleepTime=cfl.getLong(SLEEP_TIME_KEY_NAME);
      suspend=cfl.getBoolean(SUSPEND_KEY_NAME);
    }
    catch (Exception e) {
      LOG.error("exception",e);
    }
    finally {
      LOG.debug(WORKING_DIR_KEY_NAME+" "+workingDir);
      LOG.debug(LOOKUP_TIMEOUT_KEY_NAME+" "+lookupTimeout);
      LOG.debug(SLEEP_TIME_KEY_NAME+" "+sleepTime);
    }
  }
  /**
   * Check whether the domain has been updated.
   * 
   * @param fileDomain
   * @param dbDomain
   */
  private void checkDomain(Domain fileDomain, Domain dbDomain) {
    // if the IP address is error or null return and continue with the next domain 
    if (processError(fileDomain,dbDomain)) return;
    processNewDomain(fileDomain,dbDomain);
    processIpAddress(fileDomain,dbDomain);
    processDnsServer(fileDomain,dbDomain);
  }
  /**
   * Checks whether the domain's IP address is error or NULL. If so it adds the domain to the errorDomains list
   * and starts a new NSLookupThread which would try to resolve the IP address again.
   *  
   * @param fileDomain
   * @param dbDomain
   * @return true if the domain's IP address is error or NULL and it is not error pass
   */
  private boolean processError(Domain fileDomain, Domain dbDomain) {
    if (isErrorPass) return false; // domains from errorDomains are processed now
    IpAddress ipAddress=fileDomain.getIpAddress();
    if (ipAddress!=null && ipAddress.getAddress()!=null) {
      if (ipAddress.getAddress().matches(DNSHelper.IP_CHECK_REGEXP)) return false;
    }
    else { fileDomain.setIpAddress(nullIp); }
    errorDomains.put(fileDomain,dbDomain);
    resultList.add(executor.submit(new NSLookupThread(fileDomain, lookupTimeout)));
    return true;
  }
  /**
   * If the domain is not in the database it is added into the domainsToUpdate set.
   * 
   * @param fileDomain
   * @param dbDomain
   */
  private void processNewDomain(Domain fileDomain, Domain dbDomain) {
    if (dbDomain!=null) return;
    domainsToUpdate.add(fileDomain);
  }
  /**
   * Check whether the IP address has been changed.
   * @param fileDomain
   * @param dbDomain
   */
  private void processIpAddress(Domain fileDomain, Domain dbDomain) {
    IpAddress fileIpAddress=fileDomain.getIpAddress();
    if (dbDomain!=null && dbDomain.getIpAddress().equals(fileIpAddress)) return;

    Domain domainToUpdate;
    if (dbDomain!=null) domainToUpdate=dbDomain;
    else domainToUpdate=fileDomain;
   
    // Seek the resolved IP address in database.
    IpAddress dbIpAddress=(IpAddress)ses.getNamedQuery("IpAddress.findByAddress").setString("address",fileIpAddress.getAddress()).uniqueResult();
    // if it exists set is it to the domain which is being updated
    if (dbIpAddress!=null) { domainToUpdate.setIpAddress(dbIpAddress); }
    else { // if it is a new IP address
      // check if it already was in the file
      IpAddress mapIpAddress=newIpAddresses.get(fileIpAddress.getAddress());
      if (mapIpAddress!=null) { // it is already in the map, just set the ip from map to the updated domain
        domainToUpdate.setIpAddress(mapIpAddress);
      }
      else { // it wasn't in the file yet, update the domain and add it to the map of new ip addresses
        domainToUpdate.setIpAddress(fileIpAddress);
        newIpAddresses.put(fileIpAddress.getAddress(),fileIpAddress);
      }
    }
    domainsToUpdate.add(domainToUpdate);
  }
  /**
   * Check whether DNS servers have been modified for domain loaded from DB.
   * 
   * @param fileDnsSet
   * @param dbDomain
   * @return
   */
  private boolean checkDbDomianDns(Set<DnsServer> fileDnsSet, Domain dbDomain) {
    if (dbDomain!=null) { // is the domain already in database? 
      boolean modified=false;

      Set<DnsServer> dbDnsSet=dbDomain.getDnsServer();
      
      LOG.trace(fileDnsSet);
      LOG.trace(dbDnsSet);

      // go through all domain's DNS from the file 
      for (Iterator<DnsServer> fileDnsIter=fileDnsSet.iterator();fileDnsIter.hasNext();) {
        DnsServer fileDns=fileDnsIter.next();
        // if the domain from database has it just get next one
        if (dbDnsSet.contains(fileDns)) continue;
        // it's not there yet
        modified=true;
        // check whether the DNS is in the database
        DnsServer dbDns=(DnsServer)ses.getNamedQuery("DnsServer.findByName").setString("name", fileDns.getName()).uniqueResult();
        if (dbDns==null) { // it is not
          // do we have it already in the map of new DNS ?
          DnsServer mapDns=newDnsServers.get(fileDns.getName());
          if (mapDns!=null) { // no it is completely new
            dbDnsSet.add(mapDns);
          }
          else { // we have it already
            dbDnsSet.add(fileDns); // replace the new object from file by the object from the map, so we have the same object for all domains
            newDnsServers.put(fileDns.getName(), fileDns); // we will insert it
          }
        }
        else { // it is already in the database
          dbDnsSet.add(dbDns); // just replace the new object by the object from database
        }
      }
      
      // now remove all DNS which are not in the file
      for (Iterator<DnsServer> dbDnsIter=dbDnsSet.iterator();dbDnsIter.hasNext();)
        if (!fileDnsSet.contains(dbDnsIter.next())) { modified=true;dbDnsIter.remove(); } 

      LOG.trace(fileDnsSet);
      LOG.trace(dbDnsSet);

      if (modified) domainsToUpdate.add(dbDomain);
      return true;
    }
    return false;
  }
  /**
   * If the domain is new just check whether each DNS server already exists and if not insert it.
   * 
   * @param fileDnsSet
   * @param fileDomain
   */
  private void checkFileDomainDns(Set<DnsServer> fileDnsSet, Domain fileDomain) {
    Set<DnsServer> toAdd=new TreeSet<>();
    // for all DNS servers from the file
    for (Iterator<DnsServer> fileDnsIter=fileDnsSet.iterator();fileDnsIter.hasNext();) {
      DnsServer fileDns=fileDnsIter.next();
      // try to load it from database
      DnsServer dbDns=(DnsServer)ses.getNamedQuery("DnsServer.findByName").setString("name", fileDns.getName()).uniqueResult();
      if (dbDns==null) { // no such DNS server in the database
        DnsServer mapDns=newDnsServers.get(fileDns.getName());
        // is it in the map of new DNS servers for this file yet
        if (mapDns!=null) {
          toAdd.add(mapDns); // store it temporarily to add it after this loop
          fileDnsIter.remove(); // remove the file object (the one from map will be added later)
        }
        else {
          newDnsServers.put(fileDns.getName(), fileDns); // just store the file object in the map of new DNS servers for this file
        }
      }
      else { // it is in the database already
        toAdd.add(dbDns); //  store it temporarily to add it after this loop
        fileDnsIter.remove(); // remove the file object (the one from map will be added later)
      }
    }
    // now add all DNS server from the map of new DNS server or from the database
    for (DnsServer dns:toAdd) fileDnsSet.add(dns);
    domainsToUpdate.add(fileDomain); // the file domain object will be inserted
  }
  /**
   * Check whether DNS servers have been modified.
   * 
   * @param fileDomain
   * @param dbDomain
   */
  private void processDnsServer(Domain fileDomain, Domain dbDomain) {
    if (checkDbDomianDns(fileDomain.getDnsServer(), dbDomain)) return;
    checkFileDomainDns(fileDomain.getDnsServer(), fileDomain);
  }
  /**
   * Read domains from the next file and sort them out into temporary maps.
   * 
   * @param actualDir
   * @return
   * @throws IOException
   */
  private boolean readDomains(String actualDir) throws IOException {
    domainFileName=FileHelper.getNextFile(actualDir, CFG_EXT_RES); // get the next file from working directory
    if (domainFileName==null) return false; // no more files in the folder
    LOG.info("Started:  "+domainFileName);

    isErrorPass=false;
    ses=DBUtil.openSession(); // open a new DB session to load domains from database
    executor=(ExecutorService)Executors.newCachedThreadPool(); // executor for resolve domain threads
    numberOfAll=0; // number of domains in the file
    
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(actualDir+domainFileName), StandardCharsets.UTF_8)) {
      nextdomain: while (true) {
        String line=reader.readLine();
        if (line==null) break;
        Domain domain=ImportHelper.parseResInputLine(line);
        if (domain==null) continue nextdomain;
        checkDomain(domain, (Domain)ses.getNamedQuery("Domain.findByName").setString("name", domain.getName()).uniqueResult());
        numberOfAll++;
      }  
    } finally {executor.shutdown();}
    return true;
  }
  /**
   * Get results from threads re-resolving error IP addresses and sort the out.
   */
  private void checkErrors() {
    isErrorPass=true;
    errorsResolved=0;
    
    for (int index=0;;) {
      if (resultList.size()==0) break;
      // get next result from the list and if it is done process it and remove from list.
      Future<NSLookupResult> nsLookupFuture=resultList.get(index);
      if (nsLookupFuture.isDone()) {
        try {
          NSLookupResult nsLookupResult=nsLookupFuture.get();
          Domain fileDomain=nsLookupResult.getDomain();
          LOG.trace(fileDomain+" "+fileDomain.getIpAddress()+" "+errorDomains.get(fileDomain)+" "+nsLookupResult);
          if (!nsLookupResult.getIsError()) errorsResolved++;
          checkDomain(fileDomain, errorDomains.get(fileDomain));
        } catch (InterruptedException | ExecutionException e) { LOG.error("Interrupted|Execition",e); }
        resultList.remove(index);
      }
      if (++index>=resultList.size()) index=0;
    }
  }
  /**
   * Save new and modified objects into the database.
   */
  private void updateDomains() {
    LOG.debug("Begin transaction");
    Transaction tx=ses.beginTransaction();
    try {
      LOG.debug("-- new ip addresses -----------");
      for (IpAddress ipAddressToInsert:newIpAddresses.values()) {
        ses.save(ipAddressToInsert);
        LOG.debug("NEW IP: "+ipAddressToInsert);
      }
      LOG.debug("-- new dns servers ------------");
      for (DnsServer dnsServerToInsert:newDnsServers.values()) {
        ses.save(dnsServerToInsert);
        LOG.debug("NEW DNS: "+dnsServerToInsert);
      }
      LOG.debug("-- domains to update ----------");
      for (Domain domainToUpdate:domainsToUpdate) {
        ses.save(domainToUpdate);
        LOG.debug("UPD: "+domainToUpdate+" "+domainToUpdate.getIpAddress()+" "+domainToUpdate.getDnsServer());
      }
      tx.commit();
      LOG.debug("Transaction commited");
    } catch (RuntimeException e) {
      LOG.error("RT",e);
      LOG.error("Cause",e.getCause());
      for (StackTraceElement el:e.getStackTrace()) {
        LOG.error("EL:"+el.getClassName()+" "+el.getMethodName()+" "+el.getFileName()+" "+el.getLineNumber());
      }
      tx.rollback();
    }
    ses.clear();
    if (ses.isOpen()) ses.close();
    resultList.clear();
  }
  /**
   * Move processed file into the backup folder.
   * 
   * @param actualDir
   * @param backupDir
   */
  private void moveFile(final String actualDir, final String backupDir) {
    try {Files.move(Paths.get(actualDir+domainFileName),Paths.get(backupDir+domainFileName),StandardCopyOption.REPLACE_EXISTING);}
    catch (IOException e) {LOG.error("IO "+actualDir+domainFileName);}
  }
  /**
   * Print result into the log file.
   * @param start
   */
  private void logResults(long start) {
    long totalTimeInSec=(new Date().getTime()-start)/CONST1000;
    LOG.info("Total time: "+totalTimeInSec+" sec");
    LOG.info("Total domains: "+numberOfAll);
    LOG.info("Domains to update: "+domainsToUpdate.size());
    LOG.info("IP to add: "+newIpAddresses.size());
    LOG.info("DNS to add: "+newDnsServers.size());
    LOG.info("Errors: "+errorDomains.size());
    LOG.info("Errors resolved: "+errorsResolved);
    LOG.info("Domains per second: "+(totalTimeInSec==0?"-oo-":numberOfAll/totalTimeInSec));
  }
  /**
   * Clear maps.
   */
  private void cleanUp() {
    errorDomains.clear();
    domainsToUpdate.clear();
    newIpAddresses.clear();
    newDnsServers.clear();
  }
  /**
   * The executive method doJob can be called from main.
   * 
   * @param args
   */
  public static void main(String[] args) {
    try {
      new UpdateDomains().doJob(args[0]);
    } catch (Exception e) { LOG.error("Exception",e); }
  }
}