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

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.DNSHelper;
import com.gmsxo.domains.helpers.FileHelper;
import com.gmsxo.domains.helpers.ImportHelper;
import com.gmsxo.domains.resolve.v1.NSLookupThread;
import com.gmsxo.domains.resolve.v1.NSLookupThread.Result;

public class UpdateDomains {
  
  private static final Logger LOG=Logger.getLogger(UpdateDomains.class);
  //private static final Runtime RUNTIME = Runtime.getRuntime();
  
  private static final int TIMEOUT=1000;
  
  private Map<String,IpAddress> errors;

  private IpAddress NULL_IP;
  private Session ses;
  private boolean isErrorPass=false;

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      new UpdateDomains().doJob(args[0]);
    } catch (Exception e) { LOG.error("Exception",e); }
  }
  
  private Map<Domain,Domain> errorDomains=new TreeMap<>();
  private Set<Domain> domainsToUpdate=new TreeSet<>();
  private TreeMap<String,IpAddress> newIpAddresses=new TreeMap<>();
  private TreeMap<String,DnsServer>  newDnsServers=new TreeMap<>();
  
  private List<Future<Result>>resultList=new ArrayList<>();
  
  //private Map<Domain,Domain> allDomains=new TreeMap<>();
  
  private ExecutorService executor;
  
  @SuppressWarnings("unchecked")
  private void loadErrors() {
    errors=new TreeMap<String,IpAddress>();
    for (IpAddress ip:(List<IpAddress>)ses.createQuery("from IpAddress where id<100").list()) errors.put(ip.getAddress(), ip);
    
  }
  
  private void checkDomain(Domain fileDomain, Domain dbDomain) {
    //if (!isErrorPass) {
    //  allDomains.put(fileDomain,dbDomain);
    //}
    if (processError(fileDomain,dbDomain)) return;
    processNewDomain(fileDomain,dbDomain);
    processIpAddress(fileDomain,dbDomain);
    processDnsServer(fileDomain,dbDomain);
  }

  private boolean processError(Domain fileDomain, Domain dbDomain) {
    if (isErrorPass) return false;
    IpAddress ipAddress=fileDomain.getIpAddress();
    if (ipAddress!=null && ipAddress.getAddress()!=null) {
      if (ipAddress.getAddress().matches(DNSHelper.IP_CHECK_REGEXP)) return false;
    }
    else { fileDomain.setIpAddress(NULL_IP); }
    errorDomains.put(fileDomain,dbDomain);
    resultList.add(executor.submit(new NSLookupThread(fileDomain, TIMEOUT)));
    return true;
  }
  
  private void processNewDomain(Domain fileDomain, Domain dbDomain) {
    if (dbDomain!=null) return;
    domainsToUpdate.add(fileDomain);
  }
  
  private void processIpAddress(Domain fileDomain, Domain dbDomain) {
    IpAddress fileIpAddress=fileDomain.getIpAddress();
    if (dbDomain!=null && dbDomain.getIpAddress().equals(fileIpAddress)) return;

    Domain domainToUpdate;
    if (dbDomain!=null) domainToUpdate=dbDomain;
    else domainToUpdate=fileDomain;
    
    IpAddress dbIpAddress=(IpAddress)ses.getNamedQuery("IpAddress.findByAddress").setString("address",fileIpAddress.getAddress()).uniqueResult();
    if (dbIpAddress!=null) { domainToUpdate.setIpAddress(dbIpAddress); }
    else {
      IpAddress mapIpAddress=newIpAddresses.get(fileIpAddress.getAddress());
      if (mapIpAddress!=null) {
        domainToUpdate.setIpAddress(mapIpAddress);
      }
      else {
        domainToUpdate.setIpAddress(fileIpAddress);
        newIpAddresses.put(fileIpAddress.getAddress(),fileIpAddress);
      }
    }
    domainsToUpdate.add(domainToUpdate);
  }
  
  private void processDnsServer(Domain fileDomain, Domain dbDomain) {
    Set<DnsServer> fileDnsSet=fileDomain.getDnsServer();
    if (dbDomain!=null) {
      boolean modified=false;

      Set<DnsServer> dbDnsSet=dbDomain.getDnsServer();
      
      LOG.debug(fileDnsSet);
      LOG.debug(dbDnsSet);
      
      for (Iterator<DnsServer> fileDnsIter=fileDnsSet.iterator();fileDnsIter.hasNext();) {
        DnsServer fileDns=fileDnsIter.next();
        LOG.debug(fileDns);
        if (dbDnsSet.contains(fileDns)) continue;
        modified=true;
        DnsServer dbDns=(DnsServer)ses.getNamedQuery("DnsServer.findByName").setString("name", fileDns.getName()).uniqueResult();
        if (dbDns==null) {
          LOG.debug("dbDns==null "+fileDns);
          DnsServer mapDns=newDnsServers.get(fileDns.getName());
          if (mapDns!=null) {
            LOG.debug("mapDns==null "+fileDns);
            dbDnsSet.add(mapDns);
          }
          else {
            LOG.debug("else "+fileDns);
            dbDnsSet.add(fileDns);
            newDnsServers.put(fileDns.getName(), fileDns);
          }
        }
        else {
          dbDnsSet.add(dbDns);
        }
      }

      //LOG.debug(fileDnsSet);
      //LOG.debug(dbDnsSet);

      
      for (Iterator<DnsServer> dbDnsIter=dbDnsSet.iterator();dbDnsIter.hasNext();)
        if (!fileDnsSet.contains(dbDnsIter.next())) { modified=true;dbDnsIter.remove(); } 

      LOG.debug(fileDnsSet);
      LOG.debug(dbDnsSet);

      if (modified) domainsToUpdate.add(dbDomain);
      return;
    }
    
    Set<DnsServer> toAdd=new TreeSet<>();
    
    for (Iterator<DnsServer> fileDnsIter=fileDnsSet.iterator();fileDnsIter.hasNext();) {
      DnsServer fileDns=fileDnsIter.next();
      DnsServer dbDns=(DnsServer)ses.getNamedQuery("DnsServer.findByName").setString("name", fileDns.getName()).uniqueResult();
      if (dbDns==null) {
        DnsServer mapDns=newDnsServers.get(fileDns.getName());
        if (mapDns!=null) {
          toAdd.add(mapDns);
          fileDnsIter.remove();
        }
        else {
          newDnsServers.put(fileDns.getName(), fileDns);
        }
      }
      else {
        toAdd.add(dbDns);
        fileDnsIter.remove();
      }
    }
    
    for (DnsServer dns:toAdd) fileDnsSet.add(dns);
    domainsToUpdate.add(fileDomain);
  }
  
  public void doJob(final String workingDir) throws IOException {
    FileHelper.checkDirs(workingDir);
    try {

      for (String letter:ImportHelper.pref) {
        
        
        String actualDir = workingDir+CFG_EXPORT_SUB_DIR+letter+File.separator;
        String backupDir = workingDir+CFG_BACKUP_SUB_DIR+letter+File.separator;
        nextfile: while (true) {
          long start=new Date().getTime();


          String domainFileName=FileHelper.getNextFile(actualDir, CFG_EXT_RES); // get the next file from working directory
          if (domainFileName==null) break nextfile;

          isErrorPass=false;

          LOG.info("Started:  "+domainFileName);
          ses=DBUtil.openSession();
          loadErrors();
          
          executor=(ExecutorService)Executors.newCachedThreadPool();
          
          int numberOfAll=0;
          
          try (BufferedReader reader=Files.newBufferedReader(Paths.get(actualDir+domainFileName), StandardCharsets.UTF_8)) {
            nextdomain: while (true) {
              String line=reader.readLine();
              if (line==null) break;
              Domain domain=ImportHelper.parseResInputLine(line);
              if (domain==null) continue nextdomain;
              checkDomain(domain, (Domain)ses.getNamedQuery("Domain.findByName").setString("name", domain.getName()).uniqueResult());
              numberOfAll++;
            }  
          }
          
          //LOG.info("file processed, now errors:");
          //LOG.info(errorDomains);
          //LOG.info(resultList);
          
          executor.shutdown();
          isErrorPass=true;
          
          int errorsResolved=0;
          
          for (int index=0;;) {
            LOG.debug(index);
            if (resultList.size()==0) break;
            Future<Result> nsLookupFuture=resultList.get(index);
            if (nsLookupFuture.isDone()) {
              try {
                Result nsLookupResult=nsLookupFuture.get();
                Domain fileDomain=nsLookupResult.getDomain();
                LOG.debug(fileDomain+" "+fileDomain.getIpAddress()+" "+errorDomains.get(fileDomain));
                LOG.debug(nsLookupResult);
                if (!nsLookupResult.getIsError()) errorsResolved++;
                checkDomain(fileDomain, errorDomains.get(fileDomain));
              } catch (InterruptedException | ExecutionException e) { LOG.error("Interrupted|Execition",e); }
              resultList.remove(index);
            }
            if (++index>=resultList.size()) {
              index=0; try {Thread.sleep(1000);} catch (InterruptedException e) {LOG.error("Interrupted",e);}
            }
          }
          LOG.info("Begin transaction");
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
              //Domain dbDomain=allDomains.get(domainToUpdate);
              //LOG.info(" DB: "+dbDomain+" "+(dbDomain==null?"":(dbDomain.getIpAddress()+" "+dbDomain.getDnsServer())));
              
            }
            tx.commit();
            LOG.info("Transaction commited");
          } catch (RuntimeException e) {
            LOG.error("RT",e);
            LOG.error("Cause",e.getCause());
            for (StackTraceElement el:e.getStackTrace()) {
              LOG.error("EL:"+el.getClassName()+" "+el.getMethodName()+" "+el.getFileName()+" "+el.getLineNumber());
            }
            tx.rollback();
            ses.close();
            DBUtil.close();
            System.exit(-1);
          }

          //LOG.info("-- errors ---------------------");
          //for (Domain domainError:errorDomains.keySet()) LOG.info("ERR: "+domainError+" "+domainError.getIpAddress()+" "+domainError.getDnsServer());
          
          /*for (Future<Result> future:resultList)  try {
            Result result=future.get();
            LOG.info(result.getDomain()+" "+result.getIsError()+" "+result.getDomain().getIpAddress()+" "+result.getDomain().getDnsServer());
          } catch (InterruptedException | ExecutionException e) {LOG.error("Interrupted|Execution",e); }
          */
          //ses.flush();
          ses.clear();
          resultList.clear();

          ses.close();
          
          try {
            Files.move(Paths.get(actualDir+domainFileName),Paths.get(backupDir+domainFileName),StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            LOG.error("IO "+actualDir+domainFileName);
          }
          long totalTimeInSec=(new Date().getTime()-start)/1000;
          
          LOG.info("Total time: "+totalTimeInSec+" sec");
          LOG.info("Total domains: "+numberOfAll);
          LOG.info("Domains to update: "+domainsToUpdate.size());
          LOG.info("IP to add: "+newIpAddresses.size());
          LOG.info("DNS to add: "+newDnsServers.size());
          LOG.info("Errors: "+errorDomains.size());
          LOG.info("Errors resolved: "+errorsResolved);
          
          
          LOG.info("Domains per second: "+(totalTimeInSec==0?"-oo-":numberOfAll/totalTimeInSec));
          errorDomains.clear();
          domainsToUpdate.clear();
          newIpAddresses.clear();
          newDnsServers.clear();
          
          //allDomains.clear();
          
          //LOG.info("Free:" + RUNTIME.freeMemory() + "/Total:" + (RUNTIME.totalMemory()) + " c:" + numberOfAll);

          //return;
        } // end nextfile
      } //end next letter
    }
    finally {
      if (ses!=null) ses.close();
      
    }
  }
  
  private static final String formatDns(Domain domain) {
    if (domain.getDnsServer()==null) return "null";
    StringBuilder sb=new StringBuilder();
    for (DnsServer dns:new TreeSet<DnsServer>(domain.getDnsServer())) {
      sb.append(" ").append(dns.getName());
    }
    return sb.toString();
  }
  
}
