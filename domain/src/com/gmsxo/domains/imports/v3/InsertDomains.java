package com.gmsxo.domains.imports.v3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.FileHelper;

import static com.gmsxo.domains.config.AppConfig.*;

public class InsertDomains { public InsertDomains() {}
  private static Logger LOG=Logger.getLogger(InsertDomains.class);
  
  public static void main(String[] args) {
    for (String s:args) System.out.println(s);
    if (args.length!=1) {
      System.err.println("Usage InsertDomains workingDir");
      System.exit(-1);
    }

    String workingDir=args[0];

    try { new InsertDomains().doJob(workingDir); } catch (Exception e) {  LOG.error("main",e); }
    finally {  DBUtil.close();  }
  }
  

  public void doJob(String workingDir) {
    
    Thread inputFileParser,domainExporter,loadDomainsAn,loadDomainsOOthers,ipInserter,dnsInserter,domainLinkGenerator,loadDomDnsLnk=null,loadDomIpLnk=null;

    TreeSet<Domain>            domains      = new TreeSet<>();
    TreeMap<String, IpAddress> ipAddressMap = new TreeMap<>();
    TreeMap<String, DnsServer> dnsServerMap = new TreeMap<>();
    
    main: while (true) {
      
      Date time=new Date();
      long counter=0l; 
      
      domains.clear();
      ipAddressMap.clear();
      dnsServerMap.clear();
      
      String fileName=null;
      while (true) {
        if (new java.io.File(workingDir+CFG_EXPORT_SUB_DIR+"stop").exists()) {
          LOG.info("stop found");
          break main;
        }
        try { fileName=FileHelper.getNextFile(workingDir+CFG_EXPORT_SUB_DIR,CFG_EXT_RES); } catch (IOException e) { LOG.warn("getNextFile"+e); }
        if (fileName==null) { try { Thread.sleep(CFG_WAIT_FOR_NEXT_FILE_DELAY); } catch (InterruptedException e) { LOG.warn("Interrupted"+e); }}
        else break;
      }

      inputFileParser = new Thread(new InputFileParser(workingDir, fileName, domains, ipAddressMap, dnsServerMap));
      inputFileParser.start();
      
      try { inputFileParser.join(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }
      
      counter=domains.size();
      LOG.info(counter);
      LOG.info(ipAddressMap.size());
      LOG.info(dnsServerMap.size());
      
      //if (true) break main;
      domainExporter = new Thread(new DomainExporter(workingDir,domains));
      domainExporter.start();

      ipInserter = new Thread(new Inserter(ipAddressMap));
      ipInserter.start();

      dnsInserter = new Thread(new Inserter(dnsServerMap));
      dnsInserter.start();

      try { domainExporter.join(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }

      loadDomainsAn = new Thread(new PgloaderCaller(workingDir+CFG_PGLOADER_SUB_DIR+"an/pgloader.conf"));
      loadDomainsAn.start();

      loadDomainsOOthers = new Thread(new PgloaderCaller(workingDir+CFG_PGLOADER_SUB_DIR+"oother/pgloader.conf"));
      loadDomainsOOthers.start();

      try { loadDomainsAn.join(); }      catch (InterruptedException e) { LOG.error("Interrupted",e); }
      try { loadDomainsOOthers.join(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }

      try { ipInserter.join(); }  catch (InterruptedException e) { LOG.error("Interrupted",e); }
      try { dnsInserter.join(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }

      if (loadDomDnsLnk!=null) try { loadDomDnsLnk.join(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }
      if (loadDomIpLnk!=null)  try { loadDomIpLnk.join(); }  catch (InterruptedException e) { LOG.error("Interrupted",e); }

      domainLinkGenerator = new Thread(new DomainLinkGenerator(workingDir, domains));
      domainLinkGenerator.start();
      try { domainLinkGenerator.join(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }

      loadDomDnsLnk = new Thread(new PgloaderCaller(workingDir+CFG_PGLOADER_SUB_DIR+"domdns/pgloader.conf"));
      loadDomDnsLnk.start();

      loadDomIpLnk = new Thread(new PgloaderCaller(workingDir+CFG_PGLOADER_SUB_DIR+"domip/pgloader.conf"));
      loadDomIpLnk.start();      

      long totalTimeInSec=(new Date().getTime()-time.getTime())/1000l;
      LOG.info("Domains:"+counter+" Total time:"+totalTimeInSec+"sec. Per second:"+((totalTimeInSec!=0?counter/totalTimeInSec:"---"))+" "+fileName);
      LOG.info("Insert IP threads:"+CFG_INSERT_IP_THREAD_COUNT+" Insert DNS threads:"+CFG_INSERT_DNS_THREAD_COUNT+" Insert domain threads:"+CFG_INSERT_DOMAIN_THREAD_COUNT);

      if (fileName!=null)
      try {
        Files.move(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+fileName),Paths.get(workingDir+CFG_BACKUP_SUB_DIR+fileName),StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        LOG.error("IO "+workingDir+CFG_EXPORT_SUB_DIR+fileName);
      }
    }
    if (loadDomDnsLnk!=null) try { loadDomDnsLnk.join(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }
    if (loadDomIpLnk!=null)  try { loadDomIpLnk.join(); }  catch (InterruptedException e) { LOG.error("Interrupted",e); }
  }
}