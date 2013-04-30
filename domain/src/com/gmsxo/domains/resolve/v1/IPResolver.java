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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.dns.DNSLookup;
import com.gmsxo.domains.helpers.DNSHelper;
import com.gmsxo.domains.helpers.FileHelper;
import com.gmsxo.domains.helpers.ImportHelper;
import com.gmsxo.domains.resolve.ExportToFileThread;

import static com.gmsxo.domains.config.AppConfig.*;

public class IPResolver {
  public IPResolver(String workingDir) { this.workingDir=workingDir; }

  public static void main(String[] args) throws IOException, NamingException {
    try {
      /*args=new String[4];
      args[0]="C:\\Temp\\domains\\import\\";
      args[1]="25";
      args[2]="50";
      args[3]="1000";*/
      for (String str:args) System.out.println(str);
      if (args.length!=4) {
        System.err.println("Usage IPResolver workingDir threadCount domainsForFile timeout");
        System.exit(-1);
      }
      new IPResolver(args[0]).doJob(Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]));
    }
    catch (Exception e) { LOG.error("Exception",e); }
  }
  
  private static final Logger   LOG = Logger.getLogger(IPResolver.class);
  
  private String workingDir;
  private BufferedReader reader;
  private Domain lastDomain;
  private String topLevel;
  private static long requestCounter=0;
  private static long resultCounter=0;
  private static long errorCounter=0;
  private String line="";
  private int dnsSplit=2;
  
  private String getTopLevel(String fileName) {
    for (String topLevel:CFG_TOPLEVELS) if (fileName.contains(topLevel)) return topLevel;
    return null;
  }
  
  public void doJob(final int poolSize, final int domainsInfCutFile, int timeout) throws IOException {
    final int  DNS_LOOKUP_POOL_SIZE=poolSize;

    FileHelper.checkDirs(workingDir);
    cutFiles(domainsInfCutFile);
    
    while (true) {
      long startTime=new Date().getTime();
      requestCounter=0;
      errorCounter=0;
      String domainFileName=FileHelper.getNextFile(workingDir+CFG_WORKING_SUB_DIR, CFG_EXT_CUT); // get the next file from working directory
      if (domainFileName==null) break;     // if there is none -> end
      topLevel=getTopLevel(domainFileName); // init topLevel domain from the file name
      if (topLevel.equals(".us")||topLevel.equals(".biz")) dnsSplit=3;
      else dnsSplit=2;
      lastDomain=new Domain();
      
      if (topLevel==null) { // if there is no tld in the file name (e.g. ".info"), rename the file and continue with next
        Files.move(Paths.get(domainFileName), Paths.get(domainFileName+CFG_EXT_BAK), StandardCopyOption.REPLACE_EXISTING);
        continue;
      }
      
      // create thread pool and result list
      ExecutorService nsLookupPool=Executors.newFixedThreadPool(DNS_LOOKUP_POOL_SIZE);
      List<Future<NSLookupThread.Result>> nsLookupResultList=new ArrayList<>();
      
      // create and start export thread which will save the results into a file
      int threadCount=ImportHelper.pref.length;
      ExportToFileThread[] export = new ExportToFileThread[threadCount];//(workingDir+WORKING,domainFileName, workingDir+EXPORT,PART_FILE_EXT,RES_FIL_EXT);
      Thread[] exportThread = new Thread[threadCount];//(export);
      for (int i=0;i<threadCount;i++) {
        export[i]=new ExportToFileThread(workingDir+CFG_WORKING_SUB_DIR+ImportHelper.pref[i]+File.separator,domainFileName, workingDir+CFG_EXPORT_SUB_DIR+ImportHelper.pref[i]+File.separator,CFG_EXT_PART,CFG_EXT_RES);
        exportThread[i]=new Thread(export[i]);
        exportThread[i].start();
      }

      try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+CFG_WORKING_SUB_DIR+domainFileName),StandardCharsets.UTF_8)) {
        line="";
        this.reader=reader;
        
        // fill the thread pool
        for (int i=0;i<DNS_LOOKUP_POOL_SIZE&&line!=null;i++) {
          nsLookupResultList.add(nsLookupPool.submit(new NSLookupThread(getNextDomain(),timeout)));
          requestCounter++;
        }
        
        // finish the input file and continuously refill the thread pool
        // also add results to the export thread to be saved into a file
        for (int i=0;;) { // just loop
          if (nsLookupResultList.get(i).isDone()) { // if the thread is finished
            try { // try to get results
              NSLookupThread.Result result=nsLookupResultList.get(i).get();
              if (result.getDomain()!=null) export[ImportHelper.getCharIndex(result.getDomain().getName())].addDomains(result.getDomain()); // if there is a result send it to the export thread
              if (result.getIsError()) errorCounter++;
              LOG.trace(resultCounter+" / "+result.getDomain());
              resultCounter++;
              if (line!=null) { // was there still a line in the import file?
                nsLookupResultList.set(i,nsLookupPool.submit(new NSLookupThread(getNextDomain(),timeout))); // replace finished thread with a new one
                requestCounter++;
              }
              else { // there are no other domains in the file
                nsLookupResultList.remove(i); // remove finished thread
                if (nsLookupResultList.size()==0) break; // if there are no more threads, just finish
              }
            } catch (InterruptedException | ExecutionException e) { LOG.error("isDone",e); }
          }
          if (++i>=nsLookupResultList.size()) i=0; // and start again
        }
      } catch (IOException e) { LOG.error("doThreadJob1",e); }
      finally { nsLookupPool.shutdown(); }
      LOG.trace("terminate");
      while (!nsLookupPool.isTerminated())
        ;
      LOG.trace("terminatet");
      for (Thread t:exportThread) t.interrupt();
      LOG.trace("interrupted");
      Files.delete(Paths.get(workingDir+CFG_WORKING_SUB_DIR+domainFileName));
      BigDecimal totalTime=BigDecimal.valueOf((new Date().getTime()-startTime)/1000d).setScale(2, BigDecimal.ROUND_HALF_EVEN);
      BigDecimal perSecond=BigDecimal.valueOf(requestCounter).divide(totalTime, 2, BigDecimal.ROUND_HALF_EVEN);
      LOG.warn("Requests:"+requestCounter+" errors:"+errorCounter+" Total time:"+totalTime+" per sec:"+perSecond+" Threads:"+poolSize+" Timeout:"+timeout+" "+domainFileName);
    }
  }
  private Domain getNextDomain() throws IOException {
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
        dnsServer = new DnsServer(DNSLookup.formatDNS(split[dnsSplit],topLevel));
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
    LOG.trace("returned: "+returnDomain);
    return returnDomain;
  }
  /*private void checkDirs() throws IOException {
    Files.createDirectories(Paths.get(workingDir+CFG_BACKUP_SUB_DIR));
    Files.createDirectories(Paths.get(workingDir+CFG_EXPORT_SUB_DIR));
    Files.createDirectories(Paths.get(workingDir+CFG_WORKING_SUB_DIR));
    for (String letter:ImportHelper.pref) {
      Files.createDirectories(Paths.get(workingDir+CFG_WORKING_SUB_DIR+letter));
    }
    for (String letter:ImportHelper.pref) {
      Files.createDirectories(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+letter));
    }
  }*/
  private void cutFiles(int domainsInFile) throws IOException {
    for (String fileName:FileHelper.getFiles(workingDir, "")) {
      FileHelper.cutDomainFile(workingDir, fileName, workingDir+CFG_WORKING_SUB_DIR, domainsInFile, topLevel, CFG_EXT_CUT);
      Files.move(Paths.get(workingDir+fileName), Paths.get(workingDir+CFG_BACKUP_SUB_DIR+fileName), StandardCopyOption.REPLACE_EXISTING);
    }
  }
}