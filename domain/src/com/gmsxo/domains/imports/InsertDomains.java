package com.gmsxo.domains.imports;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.DNSHelper;
import com.gmsxo.domains.helpers.FileHelper;

import static com.gmsxo.domains.config.AppConfig.*;

public class InsertDomains { public InsertDomains() {}
  private static Logger LOG=Logger.getLogger(InsertDomains.class);
  private BufferedWriter outputDns;
  private BufferedWriter outputIp;
  
  public static void main(String[] args) {
    for (String s:args) System.out.println(s);
   // args=new String[]{"c:\\temp\\domains\\"};
    if (args.length!=1) {
      System.err.println("Usage InsertDomains workingDir");
      System.exit(-1);
    }

    String workingDir=args[0];

    try { new InsertDomains().doJob(workingDir); } catch (Exception e) {  LOG.error("main",e); }
    finally {  DBUtil.close();  }
  }
  
  public static void callPgloader() {
    LOG.info("Calling pgloader...");
    try {
      int retVal=0;
      String[] cmd = {"pgloader","-c", "/home/miloxe/domains/pgloader/pgloader.conf"};
      Process p = new ProcessBuilder(cmd).redirectError(Redirect.to(new File("/dev/null"))).redirectOutput(Redirect.to(new File("/dev/null"))).start();
      try { retVal=p.waitFor(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }
      LOG.debug("Pgloader returned:"+retVal);
      if (retVal!=0 && retVal!=1) {
        LOG.error("Pgloader:"+retVal);
        System.exit(-1);
      }
    } catch (IOException e) { 
      LOG.error("IO",e);
      System.exit(-1);
    }
  }
  
  public static void splitResFile(String workingDir, String fileName) {
    LOG.info("Splitting file "+fileName);
    String[] pref = new String[] {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","digit","other"};
    Set<String> ip=new TreeSet<>();
    Set<String> dns=new TreeSet<>();
    for (int i=0;i<28;i++) {
      String outputFilePathName=pref[i]+".dom";
      Set<String> domains = new TreeSet<>();
      try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+fileName),StandardCharsets.UTF_8);
          BufferedWriter output=Files.newBufferedWriter(Paths.get(workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+outputFilePathName),StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
        while (true) {
          String line=reader.readLine();
          if (line==null) break;
          String[] splitLine=line.toLowerCase().split(" ",-1);
          if (splitLine.length<3) continue;
          
          String name = splitLine[0];
          
          if (i<26) {
            if (name.startsWith(pref[i])) {
              domains.add(name);
            }
          }
          else if (i==26) { // digit
            if (name.charAt(0)>='0' && name.charAt(0)<='9') domains.add(name);
          }
          else if (i==27) {
            ip.add(splitLine[1]);
            for (int j=2;j<splitLine.length;j++) 
              dns.add(splitLine[j]);
            if (!((name.charAt(0)>='0' && name.charAt(0)<='9')||(name.charAt(0)>='a' && name.charAt(0)<='z'))) domains.add(name);
          }
        }
        for (String domain:domains) {
          output.write(domain);
          output.newLine();
        }
      } catch (IOException e) { LOG.error("IO",e); }
      finally { domains.clear(); }
    }
    
    try (BufferedWriter output=Files.newBufferedWriter(Paths.get(workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+"ip"),StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
      for (String line:ip) {
        output.write(line);
        output.newLine();
      }
    } catch (IOException e) {LOG.error("IO",e);}
    try (BufferedWriter output=Files.newBufferedWriter(Paths.get(workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+"dns"),StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
      for (String line:dns) {
        output.write(line);
        output.newLine();
      }
    } catch (IOException e) {LOG.error("IO",e);}

  }

  final List<Domain> domains=new ArrayList<>();
  final List<IpAddress> ipAddresses=new ArrayList<>();
  final List<DnsServer> dnsServers=new ArrayList<>();
  
  public void doJob(String workingDir) {
    main: while (true) {
      
      Date time=new Date();
      long counter=0l;

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
      LOG.info("Found file:"+fileName);
      //splitResFile(workingDir, fileName);
      
      //if (true) break main;
      
      //callPgloader();
      LOG.info("Processing "+fileName);
      try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+fileName),StandardCharsets.UTF_8);
          BufferedWriter outputIpLoc=Files.newBufferedWriter(Paths.get(workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+"domain_ip.lnk"),StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
          BufferedWriter outputDnsLoc=Files.newBufferedWriter(Paths.get(workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+"domain_dns.lnk"),StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
        
        this.outputIp=outputIpLoc;
        this.outputDns=outputDnsLoc;
        
        LOG.debug(this.outputIp);
        LOG.debug(this.outputDns);
        
        while (true) {
  
          String line=reader.readLine();
          if (line==null) break;
          counter++;

          //if (true) continue;
          String[] splitLine=line.split(" ",-1);
          if (splitLine.length<3) continue;
          
          Domain domain = new Domain(splitLine[0]);
          String ip=splitLine[1];
          IpAddress ipAddress = null;
          for (IpAddress ipAddressFromList:ipAddresses) if (ip.equals(ipAddressFromList.getAddress())) {
            ipAddress=ipAddressFromList;
            break;
          }
          if (ipAddress==null) {
            ipAddress=new IpAddress(ip);
            ipAddresses.add(ipAddress);
          }
          
          nextdnsserver: for (int i=2;i<splitLine.length;i++) {
            String dnsServerDomainName=splitLine[i];
            
            // this line can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
            if (!dnsServerDomainName.matches(DNSHelper.DNS_CHECK_REGEX)) continue;
            
            // this lines can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
            for (DnsServer dns:domain.getDnsServer()) {
              if (dns.getName().equals(dnsServerDomainName)) continue nextdnsserver;
            }

            DnsServer dnsServer=null;
            for (DnsServer dnsFromList:dnsServers) if (dnsServerDomainName.equals(dnsFromList.getName())) {
              dnsServer=dnsFromList;
              break;
            }
            if (dnsServer==null) {
              dnsServer=new DnsServer(dnsServerDomainName);
              dnsServers.add(dnsServer);
            }
            domain.addDnsServer(dnsServer);
          }
          
          domain.setIpAddress(ipAddress);
          domains.add(domain);
          if (counter%CFG_INSERT_DOMAIN_BATCH_SIZE==0) {
            startThreads();
          }
        }
        LOG.debug("The rest of records");
        startThreads();
      } catch (IOException e) { LOG.error("IO "+workingDir+CFG_EXPORT_SUB_DIR+fileName,e);}
      long totalTimeInSec=(new Date().getTime()-time.getTime())/1000l;
      LOG.info("Domains:"+counter+" Total time:"+totalTimeInSec+"sec. Per second:"+((totalTimeInSec!=0?counter/totalTimeInSec:"---"))+" "+fileName);
      LOG.info("Insert IP threads:"+CFG_INSERT_IP_THREAD_COUNT+" Insert DNS threads:"+CFG_INSERT_DNS_THREAD_COUNT+" Insert domain threads:"+CFG_INSERT_DOMAIN_THREAD_COUNT);
      
      if (fileName!=null)
      try {
        Files.move(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+fileName),Paths.get(workingDir+CFG_BACKUP_SUB_DIR+fileName),StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        LOG.error("IO "+workingDir+CFG_EXPORT_SUB_DIR+fileName);
      }
      
      //break;
    }
  }
  
  private void startThreads() {
    LOG.debug("= 000 ==============================================================================================================");
    List<Thread> thr1 = startIPThreads(ipAddresses, CFG_INSERT_IP_THREAD_COUNT);
    thr1.addAll(startDNSThreads(dnsServers, CFG_INSERT_DNS_THREAD_COUNT));
    for (Thread t:thr1) try { t.join(); } catch (InterruptedException e) { LOG.error("interupted",e); }

    ipAddresses.clear();
    dnsServers.clear();
    LOG.debug("= 111 ==============================================================================================================");
    for (Thread t:startDomainThreads(domains, CFG_INSERT_DOMAIN_THREAD_COUNT)) try { t.join(); } catch (InterruptedException e) { LOG.error("interupted",e); }
    LOG.debug("= 222 ==============================================================================================================");
    domains.clear();
  }
  
  private List<Thread> startDomainThreads(List<Domain> records, int parts) {
    LOG.debug("startDomainThreads");
    int length=records.size();
    if (length<parts) parts=length;
    int partLength=length/parts;
      
    int start=0;
    int end=partLength;
    List<Thread> threads=new ArrayList<Thread>();
    while (start<length) {
      List<Domain> sublist=records.subList(start, end);
      LOG.debug("outputIp:"+outputIp);
      LOG.debug("outputDns:"+outputDns);
      InsertDomainsThread it = new InsertDomainsThread(sublist,outputIp,outputDns);
      Thread t=new Thread(it);
      t.start();
      threads.add(t);
      start=end;
      end+=partLength;
      if (end>length) end=length;
    }
    return threads;
  }

  private List<Thread> startIPThreads(List<IpAddress> records, int parts) {
    LOG.debug("startIPThreads");
    int length=records.size();
    if (length<parts) parts=length;
    int partLength=length/parts;
      
    int start=0;
    int end=partLength;
    List<Thread> threads=new ArrayList<Thread>();
    while (start<length) {
      List<IpAddress> sublist=records.subList(start, end);
      InsertIPThread it = new InsertIPThread(sublist);
      Thread t=new Thread(it);
      t.start();
      threads.add(t);
      start=end;
      end+=partLength;
      if (end>length) end=length;
    }
    return threads;
  }
  
  private List<Thread> startDNSThreads(List<DnsServer> records, int parts) {
    LOG.debug("startDNSThreads");
    int length=records.size();
    if (length<parts) parts=length;
    int partLength=length/parts;
    int start=0;
    int end=partLength;

    List<Thread> threads=new ArrayList<Thread>();
    while (start<length) {
      List<DnsServer> sublist=records.subList(start, end);
      InsertDNSThread it = new InsertDNSThread(sublist);
      Thread t=new Thread(it);
      t.start();
      threads.add(t);
      start=end;
      end+=partLength;
      if (end>length) end=length;
    }
    return threads;
  }
}

class InsertDomainsThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(InsertDomainsThread.class);

  public InsertDomainsThread(List<Domain> domains, BufferedWriter outputIp, BufferedWriter outputDns) {
    this.domains=domains;
    this.outputIp=outputIp;
    this.outputDns=outputDns;
  }
  private BufferedWriter outputIp;
  private BufferedWriter outputDns;
  private List<Domain> domains;
  @Override
  public void run() {
    LOG.debug("= domains start ====================================================================================================");
    int errorDomainCounter=0;

    while (true) {
      Session domainSession = DBUtil.openSession();
      Domain err=null;
      if (errorDomainCounter>CFG_MAX_ERRORS) {
        LOG.error("something's wrong.");
        System.exit(-1);
      }
      try {
        domainSession.beginTransaction();
        for (Domain d:domains) {
          boolean save=false;
          LOG.debug("1=="+d);
          err=d;
          
          LOG.debug("Domain:"+d);
          
          if (d!=null) {
            LOG.debug("ip:"+d.getIpAddress());
            LOG.debug("dns:"+d.getDnsServer());
            if (d.getDnsServer()!=null) for (DnsServer dns:d.getDnsServer()) LOG.debug(dns);
          }
          
          Domain loadedDomain=(Domain)domainSession.createQuery("from Domain d where d.name=:name")
                                  .setParameter("name", d.getName()).uniqueResult();
          
          LOG.debug("LoadDomain:"+loadedDomain);
          if (loadedDomain!=null) {
            LOG.debug("ip:"+loadedDomain.getIpAddress());
            LOG.debug("dns:"+loadedDomain.getDnsServer());
            if (loadedDomain.getDnsServer()!=null) for (DnsServer dns:loadedDomain.getDnsServer()) LOG.debug(dns);
          }
          
          if (loadedDomain==null) {
            LOG.warn("Not found:"+d);
            continue;
          }
          
          if (d.getIpAddress()==null) {
            LOG.warn("IP Address is null: "+d);
            continue;
          }
          
          if (d.getIpAddress().getId()==null) {
            LOG.warn("IP  Address Id is null: "+d+" "+d.getIpAddress());
            continue;
          }

          if (outputIp==null) {
            LOG.warn("outputIp is null");
            continue;
          }
          
          outputIp.write(loadedDomain.getId()+";"+d.getIpAddress().getId());
          outputIp.newLine();
          
          if (d.getDnsServer()==null) {
            LOG.warn("No dns servers: "+d);
            continue;
          }
          
          for (DnsServer dns:d.getDnsServer()) {
            if (dns.getId()==null) {
              LOG.warn("dns id is null: "+d+" "+dns);
              continue;
            }
            
            if (outputDns==null) {
              LOG.warn("outputDns is null");
              continue;
            }

            outputDns.write(loadedDomain.getId()+";"+dns.getId());
            outputDns.newLine();
          }
          
          /*if (loadedDomain==null) { domainSession.save(d); }
          else {
            if (loadedDomain.getIpAddress()==null||loadedDomain.getIpAddress().getId()!=d.getIpAddress().getId()) {
              loadedDomain.setIpAddress(d.getIpAddress());
              save=true;
            }
            if (loadedDomain.getDnsServer().size()!=d.getDnsServer().size()) {
              loadedDomain.setDnsServer(d.getDnsServer());
              save=true;
            }
            else {
              nextDNS: for (DnsServer loadedDNS:loadedDomain.getDnsServer()) {
                for (DnsServer dns:d.getDnsServer()) if (loadedDNS.getId()==dns.getId()) continue nextDNS;
                loadedDomain.setDnsServer(d.getDnsServer());
                save=true;
              }
            }
          }
          if (save) domainSession.save(loadedDomain);*/
          LOG.debug("2=="+loadedDomain);
        }
        domainSession.getTransaction().commit();
        domainSession.clear();
        errorDomainCounter=0;
      }
      catch (Exception e) { 
        LOG.error("insert "+err,e);
        domainSession.getTransaction().rollback();
        if (errorDomainCounter==0) for (Domain d:domains) LOG.error("Rollback:"+d);
        errorDomainCounter++;
        continue;
      }
      finally { domainSession.close(); }
      break;
    }
    LOG.debug("= domains end ======================================================================================================");
  }
}

class InsertIPThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(InsertIPThread.class);
  private List<IpAddress> ipAddresses;

  public InsertIPThread(List<IpAddress> records) { this.ipAddresses=records; }
  
  public static final String _DB_INSERT_IP_TEMPLATE="with insert_ip_check as ("+
      "insert into IP_ADDRESS (\"address\") "+
      "select '%s' "+
      "where not exists (select id from IP_ADDRESS where ADDRESS='%s') "+
      "returning IP_ADDRESS.id, IP_ADDRESS.address"+
    ") "+
    "select id,address from insert_ip_check "+ 
    "union select id,address from IP_ADDRESS where address='%s'";


  @Override
  public void run() {
    LOG.debug("= ip start =========================================================================================================");
    int errorIPCounter=0;
    while (true) {
      Session session = DBUtil.openSession();
      IpAddress err=null;
      try {
        session.beginTransaction();
        for (IpAddress ipa:ipAddresses) {
          err=ipa;
          LOG.debug(ipa);
          if (ipa.getId()==null) {
            String addr=ipa.getAddress().substring(0,Math.min(15, ipa.getAddress().length()));
            ipa.setId(((IpAddress)session.createSQLQuery(String.format(_DB_INSERT_IP_TEMPLATE, addr, addr, addr))
                                          .addEntity(IpAddress.class).uniqueResult()).getId());
            LOG.debug("saved:"+ipa);
          }
        }
        session.getTransaction().commit();
        session.clear();
        LOG.debug("*  IP  STOP *******************************************************************");
        break;
      }
      catch (Exception e) {
        LOG.error("ipTH1 "+err,e);
        session.getTransaction().rollback();
        if (errorIPCounter==0) for (IpAddress ip:ipAddresses) LOG.error("Rollback:"+ip);
        if (errorIPCounter++>CFG_MAX_ERRORS) {
          LOG.error("Something's wrong");
          System.exit(-1);
        }
      }
      finally { session.close(); }
    }
    LOG.debug("= ip end ===========================================================================================================");
  }
}

class InsertDNSThread implements Runnable {
  
  public static final String _DB_INSERT_DNS_TEMPLATE="with insert_dns_check as ("+
      "insert into DNS_SERVER (\"name\") "+
      "select '%s' "+
      "where not exists (select id from DNS_SERVER where NAME='%s') "+
      "returning DNS_SERVER.id, DNS_SERVER.name"+
    ") "+
    "select id,name from insert_dns_check "+ 
    "union select id,name from DNS_SERVER where name='%s'";
  
  Logger LOG=Logger.getLogger(InsertDNSThread.class);
  private List<DnsServer> dnsServers;
  public InsertDNSThread(List<DnsServer> records) { this.dnsServers=records; }
  @Override
  public void run() {
    LOG.debug("= dns start ========================================================================================================");
    int errorDNSCounter=0;
    while (true) {
      Session session = DBUtil.openSession();
      DnsServer err=null;
      try { session.beginTransaction();
        for (DnsServer dns:dnsServers) {
          err=dns;
          LOG.debug(dns);
          if (dns.getId()==null) {
            DnsServer loadedDns=(DnsServer)session.createSQLQuery(String.format(_DB_INSERT_DNS_TEMPLATE, dns.getName(), dns.getName(), dns.getName()))
                .addEntity(DnsServer.class).uniqueResult();
            dns.setId(loadedDns.getId());
            LOG.debug("saved:"+dns);
          }
        }
        session.getTransaction().commit();
        session.clear();
        LOG.debug("*  DNS STOP *******************************************************************");
        break;
      }
      catch (Exception e) { 
        LOG.error("DNS "+err,e);
        session.getTransaction().rollback();
        if (errorDNSCounter==0) for (DnsServer dns:dnsServers) LOG.error("Rollback:"+dns);
        if (errorDNSCounter++>CFG_MAX_ERRORS) {
          LOG.error("Something's wrong");
          System.exit(-1);
        }
      }
      finally { session.close(); }
    }
    LOG.debug("= dns end ==========================================================================================================");
  }
}