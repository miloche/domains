package com.gmsxo.domains.resolve.v1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.gmsxo.domains.config.ConfigLoader;
import com.gmsxo.domains.controller.DaemonThreadFactory;
import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.dns.DNSLookup;
import com.gmsxo.domains.helpers.DNSHelper;
import com.gmsxo.domains.helpers.FileHelper;
import com.gmsxo.domains.helpers.ImportHelper;
//import com.gmsxo.domains.resolve.ExportToFileThread;

import static com.gmsxo.domains.config.AppConfig.*;

public class IPResolver implements Runnable {
  private static final Logger LOG = Logger.getLogger(IPResolver.class);
  private static final int SPLIT_COM=3;
  private static final int SPLIT_OTHER=2;
  private static final int SLEEP=10;
  private static final double DIVIDE1000=1000d;
  
  private static final String SLEEP_TIME_KEY_NAME="SLEEPTIME";
  private static final String WORKING_DIR_KEY_NAME="WORKINGDIR";
  private static final String POOL_SIZE_KEY_NAME="POOLSIZE";
  private static final String DOMIANS_IN_FILE_KEY_NAME="DOMAINSINFILE";
  private static final String LOOKUP_TIMEOUT_KEY_NAME="LOOKUPTIMEOUT";
  private static final String CUT_ONLY_KEY_NAME="CUTONLY";
  private static final String SUSPEND_KEY_NAME="SUSPEND";
  
  private static final String EXP_TH_NAME_ROOT="EXPT";
  
  private long sleepTime;
  private int poolSize;
  private int domainsInFile;
  private int lookupTimeout;
  private boolean cutOnly;
  private boolean suspend;
  
  private String workingDir;
  private String domainFileName;
  private BufferedReader reader;
  private String line;
  
  private Domain lastDomain;
  private String topLevel;
  private int dnsSplit;

  private long requestCounter;
  private long resultCounter;
  private long errorCounter;
  
  private long startTime;

  private ExecutorService nsLookupPool;
  private List<Future<NSLookupThread.NSLookupResult>> nsLookupResultList;
  private ExportToFileThread[] export;
  private Thread[] exportThread;
  
  public IPResolver() {}
  public IPResolver(String workingDir) { this.workingDir=workingDir; }

  public void doJob(final int poolSize, final int domainsInfCutFile, int timeout) throws IOException, InterruptedException {
    LOG.debug("IPResolver.doJob("+workingDir+")");

    FileHelper.checkDirs(workingDir);
    cutFiles(domainsInfCutFile);
    if (cutOnly) return;
    
    while (true) {
      startTime=new Date().getTime();
      if (!nextFile()||Thread.interrupted()) {
        if (Thread.interrupted()) LOG.debug("Thread.interrupted()");
        break;
      }
      if (!checkTld()) continue;
      init();
      initExportToFileThreads();
      readDomains();
      cleanUp();
      logResults();
    }
    LOG.debug("IPResolver.doJob() end");
  }
  @Override
  public void run() {
    LOG.debug("IPResolver run");
    try {
      while (true) {
        loadConfig();
        try { Thread.sleep(sleepTime); } catch (InterruptedException e) { 
          LOG.error("interrupted suspend",e);
          Thread.currentThread().interrupt();
          return;
        }
        if (!suspend) try { doJob(poolSize,domainsInFile,lookupTimeout); } catch (IOException e) { LOG.error("IO",e); } catch (InterruptedException e) {
          LOG.error("interrupted not suspend",e);
        }
      }
    } finally {LOG.info("Thread end.");}
  }
  /**
   * Loads the configuration from database.
   */
  private void loadConfig() {
    try {
      ConfigLoader cfl=ConfigLoader.loadConfig(getClass().getName());
      workingDir=cfl.getString(WORKING_DIR_KEY_NAME);
      poolSize=cfl.getInteger(POOL_SIZE_KEY_NAME);
      sleepTime=cfl.getLong(SLEEP_TIME_KEY_NAME);
      domainsInFile=cfl.getInteger(DOMIANS_IN_FILE_KEY_NAME);
      lookupTimeout=cfl.getInteger(LOOKUP_TIMEOUT_KEY_NAME);
      cutOnly=cfl.getBoolean(CUT_ONLY_KEY_NAME);
      suspend=cfl.getBoolean(SUSPEND_KEY_NAME);
    }
    catch (Exception e) {LOG.error("exception",e);}
    finally {
      LOG.debug(WORKING_DIR_KEY_NAME+" "+workingDir);
      LOG.debug(POOL_SIZE_KEY_NAME+" "+poolSize);
      LOG.debug(SLEEP_TIME_KEY_NAME+" "+sleepTime);
      LOG.debug(DOMIANS_IN_FILE_KEY_NAME+" "+domainsInFile);
      LOG.debug(LOOKUP_TIMEOUT_KEY_NAME+" "+lookupTimeout);
    }
  }
  /**
   * Get the next file from working directory. Return true if it exists.
   * 
   * @return
   * @throws IOException
   */
  private boolean nextFile() throws IOException {
    if ((domainFileName=FileHelper.getNextFile(workingDir+CFG_WORKING_SUB_DIR, CFG_EXT_CUT))==null) return false;     // if there is none -> end
    return true;
  }
  /**
   * Initialise topLevel domain from the file name and return true if it exists.
   * If there is no TLD in the file name (e.g. ".info"), rename the file and continue with next one.
   * 
   * @return
   * @throws IOException
   */
  private boolean checkTld() throws IOException {
    if ((topLevel=getTopLevel(domainFileName))==null) { // 
      Files.move(Paths.get(domainFileName), Paths.get(domainFileName+CFG_EXT_BAK), StandardCopyOption.REPLACE_EXISTING);
      return false;
    }
    return true;
  }
  /**
   * Fills the pool with thread and starts them.
   */
  private void initExportToFileThreads() {
    LOG.debug("initThreads()");
    // create thread pool and result list
    nsLookupPool=Executors.newFixedThreadPool(poolSize, new ThreadFactory() {

      @Override
      public Thread newThread(Runnable r) {
        final Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName("NSLKP"+requestCounter);
        return t;
      }
      
    });
    nsLookupResultList=new ArrayList<>();
    int threadCount=ImportHelper.pref.length;
    export = new ExportToFileThread[threadCount];
    exportThread = new Thread[threadCount];
    for (int i=0;i<threadCount;i++) {
      export[i]=new ExportToFileThread(workingDir+CFG_WORKING_SUB_DIR+ImportHelper.pref[i]+File.separator,domainFileName, workingDir+CFG_EXPORT_SUB_DIR+ImportHelper.pref[i]+File.separator,CFG_EXT_PART,CFG_EXT_RES);
      exportThread[i]=new Thread(export[i]);
      exportThread[i].setDaemon(true);
      exportThread[i].setName(EXP_TH_NAME_ROOT+ImportHelper.pref[i]);
      exportThread[i].setUncaughtExceptionHandler(new ExportToFileThread.ExceptionHandler());
      exportThread[i].start();
      LOG.trace(exportThread[i]+" started");
    }
    LOG.debug("initThreads() end");
  }
  /**
   * Submit threads into NS lookup thread pool.
   *  
   * @throws IOException
   */
  private void fillNSLookupThreadPool() throws IOException {
    LOG.debug("fillPool()");
    for (int i=0;i<poolSize&&line!=null;i++) {
      nsLookupResultList.add(nsLookupPool.submit(new NSLookupThread(getNextDomain(),lookupTimeout)));
      requestCounter++;
    }
    LOG.debug("fillPool() end");
  }
  /**
   * Reads the input file and continuously refill the thread pool and puts results to the export threads to be saved into a file.
   * @throws InterruptedException 
   * 
   * @throws IOException
   */
  private void readDomains() throws InterruptedException {
    LOG.debug("readDomains()");
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+CFG_WORKING_SUB_DIR+domainFileName),StandardCharsets.UTF_8)) {
      this.reader=reader;
      fillNSLookupThreadPool();
      int i=0;
      while (true) { // just loop
        if (Thread.currentThread().isInterrupted()) {
          LOG.info("INTERRUPTED");
          for (Thread et:exportThread) et.interrupt();
          nsLookupPool.shutdownNow();
          throw new InterruptedException();
        }
        if (nsLookupResultList.get(i).isDone()) { // if the thread is finished
          try { // try to get results
            NSLookupThread.NSLookupResult result=nsLookupResultList.get(i).get();
            // if there is a result send it to the export thread
            if (result.getDomain()!=null) export[ImportHelper.getCharIndex(result.getDomain().getName())].addDomain(result.getDomain());
            if (result.getIsError()) errorCounter++;
            resultCounter++;
            LOG.trace(resultCounter+" / "+result.getDomain());
            if (line!=null) { // is there a new line in the import file?
              nsLookupResultList.set(i,nsLookupPool.submit(new NSLookupThread(getNextDomain(),lookupTimeout))); // replace finished thread with a new one
              requestCounter++;
            }
            else { // there are no more domains in the file
              nsLookupResultList.remove(i); // remove finished thread
              if (nsLookupResultList.size()==0) break; // if there are no more threads, just finish
            }
          } catch (Exception e) { LOG.error("excpetion",e); }
        }
        if (++i>=nsLookupResultList.size()) i=0; // and start again
      }
    } catch (IOException e) { LOG.error("doJob",e); }
    LOG.debug("readDomains() end");
  }
  /**
   * Reads the next domain from the input file.
   * 
   * @return the next domain
   * @throws IOException
   */
  private Domain getNextDomain() throws IOException {
    LOG.debug("getNextDomain()");
    Domain returnDomain = null;
    DnsServer dnsServer=null;
    String domainName=null;
    while (true) {
      line = reader.readLine();
      if (line!=null) {
        if (!line.matches(DNSHelper.DOMAIN_IN_IMPORT_REGEX)) continue;
        String[] split=line.split(" ",-1);
        if (split.length<2) return null;
        domainName=DNSLookup.formatDomain(split[0],topLevel);
        dnsServer=new DnsServer(DNSLookup.formatDNS(split[dnsSplit],topLevel));
      }
      if (line!=null&&domainName.equals(lastDomain.getName())) { // it is the same domain, add DNS server and continue
        lastDomain.addDnsServer(dnsServer);
        continue;
      } else { // it is the next domain, create new domain, update return domain and update the last domain with the new one
        Domain domain=new Domain();
        domain.setName(domainName);
        domain.addDnsServer(dnsServer);
        
        if (lastDomain.getName()==null) { // the first line
          lastDomain=domain;
          continue;
        }
        returnDomain=lastDomain;
        lastDomain=domain;
        break;
      }
    }
    LOG.debug("getNextDomain() end: "+returnDomain);
    return returnDomain;
  }
  private void init() {
    LOG.debug("init()");
    if (topLevel.equals(".us")||topLevel.equals(".biz")) dnsSplit=SPLIT_COM;
    else dnsSplit=SPLIT_OTHER;
    lastDomain=new Domain();
    requestCounter=0;
    errorCounter=0;
    line="";
    LOG.debug("init() end");
  }
  /**
   * Stops threads and moves the input file.
   * 
   * @throws IOException
   */
  private void cleanUp() throws IOException {
    LOG.debug("cleanUp()");
    nsLookupPool.shutdown();
    while (!nsLookupPool.isTerminated()) try {Thread.sleep(SLEEP);} catch (InterruptedException e) { LOG.error("Interrupted",e); }
    for (ExportToFileThread exportThread:export) exportThread.finished();
    for (Thread t:exportThread) t.interrupt();
    Files.delete(Paths.get(workingDir+CFG_WORKING_SUB_DIR+domainFileName));
    LOG.debug("cleanUp() end");
  }
  private void logResults() {
    LOG.debug("logResults()");
    BigDecimal totalTime=BigDecimal.valueOf((new Date().getTime()-startTime)/DIVIDE1000).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    BigDecimal perSecond=BigDecimal.valueOf(requestCounter).divide(totalTime, 2, BigDecimal.ROUND_HALF_EVEN);
    LOG.info("Requests:"+requestCounter+" errors:"+errorCounter+" Total time:"+totalTime+" per sec:"+perSecond+" Threads:"+poolSize+" Timeout:"+lookupTimeout+" "+domainFileName);
    LOG.debug("logResults() end");
  }
  /**
   * Cuts the import file into a pieces with given number of domains.
   * 
   * @param domainsInFile
   * @throws IOException
   */
  private void cutFiles(int domainsInFile) throws IOException {
    for (String fileName:FileHelper.getFiles(workingDir, "")) {
      FileHelper.cutDomainFile(workingDir, fileName, workingDir+CFG_WORKING_SUB_DIR, domainsInFile, topLevel, CFG_EXT_CUT);
      Files.move(Paths.get(workingDir+fileName), Paths.get(workingDir+CFG_BACKUP_SUB_DIR+fileName), StandardCopyOption.REPLACE_EXISTING);
    }
  }
  /**
   * Extracts the top level domain from the file name.
   * 
   * @param fileName
   * @return
   */
  private static String getTopLevel(String fileName) {
    for (String topLevel:CFG_TOPLEVELS) if (fileName.contains(topLevel)) return topLevel;
    return null;
  }

  public static void main(String[] args) throws IOException, NamingException {
    final int params=4;
    final int p1=1;
    final int p2=2;
    final int p3=3;
    try {
      /*args=new String[4];
      args[0]="C:\\Temp\\domains\\import\\";
      args[P1]="25";
      args[P2]="50";
      args[P3]="1000";*/
      for (String str:args) System.out.println(str);
      if (args.length!=params) {
        System.err.println("Usage IPResolver workingDir threadCount domainsForFile timeout");
        System.exit(-1);
      }
      new IPResolver(args[0]).doJob(Integer.parseInt(args[p1]),Integer.parseInt(args[p2]),Integer.parseInt(args[p3]));
    }
    catch (Exception e) { LOG.error("Exception",e); }
  }
}