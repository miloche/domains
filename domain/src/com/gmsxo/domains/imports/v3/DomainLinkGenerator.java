package com.gmsxo.domains.imports.v3;

import static com.gmsxo.domains.config.AppConfig.*;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.imports.v2.InsertDomains;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DomainLinkGenerator implements Runnable {
  private static final Logger LOG=Logger.getLogger(DomainLinkGenerator.class);
  
  private TreeSet<Domain> domains;
  private String workingDir;
  
  public DomainLinkGenerator(String workingDir, TreeSet<Domain> domains) { this.workingDir=workingDir;this.domains=domains; }
  @Override
  public void run() {
    try {
      LOG.debug("DomainLinkGenerator started:"+domains.size());
      ExecutorService insertIpPool=Executors.newFixedThreadPool(CFG_INSERT_DOMAIN_THREAD_COUNT);
      Domain from=domains.first();
      int counter=0;
      int fileIndex=0;
      for (Domain rec:domains) {
        if (counter++==CFG_INSERT_DOMAIN_THREAD_RECORDS) {
          insertIpPool.submit(new DomainLinkGeneratorThread(workingDir,fileIndex++,domains.subSet(from, rec)));
          counter=1;
          from=rec;
        }
      }
      insertIpPool.submit(new DomainLinkGeneratorThread(workingDir,fileIndex,domains.tailSet(from)));
      insertIpPool.shutdown();
      createPgloaderConf(fileIndex,"domdns");
      createPgloaderConf(fileIndex,"domip");
      try {
        insertIpPool.awaitTermination(1, TimeUnit.HOURS);
      } catch (InterruptedException e) { LOG.error("Interrupted", e);}
    } catch (Exception e) { LOG.error("Exception",e); }
    LOG.debug("DomainLinkGenerator finished");
  }
  
  private void createPgloaderConf(int files, String entity) {
    String configFileName = workingDir+CFG_PGLOADER_SUB_DIR+entity+"/pgloader.conf";
    try {
      Files.copy(Paths.get(workingDir+CFG_PGLOADER_SUB_DIR+entity+"/pgloader.conf.tmpl"),Paths.get(configFileName),StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) { LOG.error("IO",e);}
    
    String linkFileName = workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+entity+".";
    
    try (BufferedWriter config=Files.newBufferedWriter(Paths.get(configFileName), StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
      config.newLine();
      
      for (int i=0;i<=files;i++) {
        config.write("[s"+String.format("%04d", i)+"]");
        config.newLine();
        config.write("use_template = "+entity);
        config.newLine();
        config.write("filename = "+linkFileName+String.format("%04d", i)+".lnk");
        config.newLine();
        config.newLine();
      }
    } catch (IOException e) { LOG.error("IO",e);}
  }
}

class DomainLinkGeneratorThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(DomainLinkGeneratorThread.class);
  private SortedSet<Domain> domains;
  private String workingDir;
  private int fileIndex;
  public DomainLinkGeneratorThread(String workingDir,int fileIndex,SortedSet<Domain> domains) {
    this.workingDir=workingDir;
    this.fileIndex=fileIndex;
    this.domains=domains;
  }

  @Override
  public void run() {
    LOG.debug("DomainLinkGeneratorThread started:"+domains.size());
    String query ="select id from domain_%s where name='%s'";
    String  ipLnkFileName=workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+"domip." +String.format("%04d", fileIndex)+".lnk";
    String dnsLnkFileName=workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+"domdns."+String.format("%04d", fileIndex)+".lnk";
    Session ses = DBUtil.openSession();
    
    try (BufferedWriter fileIp =Files.newBufferedWriter(Paths.get( ipLnkFileName), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
         BufferedWriter fileDns=Files.newBufferedWriter(Paths.get(dnsLnkFileName), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
      for (Domain d:domains) {
        Integer domainId = ((Number)ses.createSQLQuery(String.format(query,InsertDomains.getDomPartSuff(d.getName()),d.getName())).uniqueResult()).intValue();
        fileIp.write(domainId+";"+d.getIpAddress().getId());
        fileIp.newLine();

        for (DnsServer dns:d.getDnsServer()) {
          fileDns.write(domainId+";"+dns.getId());
          fileDns.newLine();
        }
      }
    } catch (IOException e) { LOG.error("IO",e); }
    catch (Exception e) { LOG.error("Exception",e); }
    finally {ses.close();}
    LOG.debug("DomainLinkGeneratorThread finished");
  }
}
