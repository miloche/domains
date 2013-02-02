package com.gmsxo.domains.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.DNSServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IPAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.FileHelper;

import static com.gmsxo.domains.config.AppConfig.*;
import static com.gmsxo.domains.db.DBFacade.*;

public class InsertDomains { public InsertDomains() {}
  private static Logger LOG=Logger.getLogger(InsertDomains.class);
  public static void main(String[] args) {
    for (String s:args) System.out.println(s);
    if (args.length!=1) {
      System.err.println("Usage InsertDomains workingDir");
      System.exit(-1);
    }
    //args=new String[]{"c:\\temp\\domains\\full-zone.biz.ks392bNq9dt.001.cut.res"};
    String fileName=args[0];
    try { new InsertDomains().doJob(fileName); } catch (Exception e) {  LOG.error("main",e); }
    finally {  DBUtil.close();  }
  }

  public void doJob(String workingDir) {
    List<Domain> domains=new ArrayList<>();
    final List<IPAddress> ipAddresses=new ArrayList<>();
    final List<DNSServer> dnsServers=new ArrayList<>();
    
    while (true) {
      Date time=new Date();
      long counter=0l;

      String fileName=null;
      while (true) {
        try { fileName=FileHelper.getNextFile(workingDir+CFG_EXPORT_SUB_DIR,CFG_EXT_RES); } catch (IOException e) { LOG.warn("getNextFile"+e); }
        if (fileName==null) { try { Thread.sleep(CFG_WAIT_FOR_NEXT_FILE_DELAY); } catch (InterruptedException e) { LOG.warn("Interrupted"+e); }}
        else break;
      }
      try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+fileName),StandardCharsets.UTF_8)) {
        while (true) {
          counter++;
  
          String line=reader.readLine();
          if (line==null) break;
          if (counter<0) continue;
          String[] splitLine=line.split(" ",-1);
          if (splitLine.length<3) continue;
          
          Domain domain = new Domain(splitLine[0]);
          String ip=splitLine[1];
          IPAddress ipAddress = null;
          for (IPAddress ipAddressFromList:ipAddresses) if (ip.equals(ipAddressFromList.getIpAddress())) {
            ipAddress=ipAddressFromList;
            break;
          }
          if (ipAddress==null) {
            ipAddress=new IPAddress(ip);
            ipAddresses.add(ipAddress);
          }
          
          nextdomain: for (int i=2;i<splitLine.length;i++) {
            String dnsServerDomainName=splitLine[i];
            
            // this line can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
            
            
            for (DNSServer dns:domain.getDnsServer()) {
              if (dns.getDomainName().equals(dnsServerDomainName)) continue nextdomain;
            }

            DNSServer dnsServer=null;
            for (DNSServer dnsFromList:dnsServers) if (dnsServerDomainName.equals(dnsFromList.getDomainName())) {
              dnsServer=dnsFromList;
              break;
            }
            if (dnsServer==null) {
              dnsServer=new DNSServer(dnsServerDomainName);
              dnsServers.add(dnsServer);
            }
            domain.addDnsServer(dnsServer);
          }
          
          domain.setIPAddress(ipAddress);
          domains.add(domain);
          if (counter%CFG_INSERT_DOMAIN_BATCH_SIZE==0) {
  
            List<Thread> thr = startIPThreads(ipAddresses, CFG_INSERT_IP_THREAD_COUNT);
            thr.addAll(startDNSThreads(dnsServers, CFG_INSERT_DNS_THREAD_COUNT));
            
            for (Thread t:thr) try { t.join(); } catch (InterruptedException e) { LOG.error("interupted",e); }
            
            ipAddresses.clear();
            dnsServers.clear();
            LOG.debug("====================================================================================================================");
            
            for (Thread t:startDomainThreads(domains, CFG_INSERT_DOMAIN_THREAD_COUNT)) try { t.join(); } catch (InterruptedException e) { LOG.error("interupted",e); }
            
            domains.clear();
          }
        }
      } catch (IOException e) { LOG.error("IO "+workingDir+CFG_EXPORT_SUB_DIR+fileName,e);}
      long totalTimeInSec=(new Date().getTime()-time.getTime())/1000l;
      LOG.info("Domains: "+counter+" Time: "+totalTimeInSec+" Per second: "+(counter/totalTimeInSec)+" "+fileName);
      
      if (fileName!=null)
      try {
        Files.move(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+fileName),Paths.get(workingDir+CFG_BACKUP_SUB_DIR+fileName));
      } catch (IOException e) {
        LOG.error("IO "+workingDir+CFG_EXPORT_SUB_DIR+fileName);
      }
      
      //break;
    }
  }
  private List<Thread> startDomainThreads(List<Domain> records, int parts) {
    int length=records.size();
    int partLength=length/parts;
      
    int start=0;
    int end=partLength;
    List<Thread> threads=new ArrayList<Thread>();
    while (start<length) {
      List<Domain> sublist=records.subList(start, end);
      InsertDomainsThread it = new InsertDomainsThread(sublist);
      Thread t=new Thread(it);
      t.start();
      threads.add(t);
      start=end;
      end+=partLength;
      if (end>length) end=length;
    }
    return threads;
  }

  private List<Thread> startIPThreads(List<IPAddress> records, int parts) {
    int length=records.size();
    int partLength=length/parts;
      
    int start=0;
    int end=partLength;
    List<Thread> threads=new ArrayList<Thread>();
    while (start<length) {
      List<IPAddress> sublist=records.subList(start, end);
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
  
  private List<Thread> startDNSThreads(List<DNSServer> records, int parts) {
    int length=records.size();
    int partLength=length/parts;
    int start=0;
    int end=partLength;

    List<Thread> threads=new ArrayList<Thread>();
    while (start<length) {
      List<DNSServer> sublist=records.subList(start, end);
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

  public InsertDomainsThread(List<Domain> domains) {this.domains=domains;}
  
  private List<Domain> domains;
  @Override
  public void run() {
    int errorCounter=0;

    while (true) {
      Session domainSession = DBUtil.openSession();
      Domain err=null;
      if (errorCounter>CFG_MAX_ERRORS) {
        LOG.error("something's wrong.");
        System.exit(-1);
      }
      try {
        domainSession.beginTransaction();
        for (Domain d:domains) {
          boolean save=false;
          LOG.debug("1=="+d);
          err=d;
          Domain loadedDomain=(Domain)domainSession.createQuery("from Domain d where d.domainName=:domainName")
                                  .setParameter("domainName", d.getDomainName()).uniqueResult();
          
          if (loadedDomain==null) { domainSession.save(d); }
          else {
            if (loadedDomain.getIpAddress().getId()!=d.getIpAddress().getId()) {
              loadedDomain.setIPAddress(d.getIpAddress());
              save=true;
            }
            if (loadedDomain.getDnsServer().size()!=d.getDnsServer().size()) {
              loadedDomain.setDnsServer(d.getDnsServer());
              save=true;
            }
            else {
              nextDNS: for (DNSServer loadedDNS:loadedDomain.getDnsServer()) {
                for (DNSServer dns:d.getDnsServer()) if (loadedDNS.getId()==dns.getId()) continue nextDNS;
                loadedDomain.setDnsServer(d.getDnsServer());
                save=true;
              }
            }
          }
          if (save) domainSession.save(loadedDomain);
          LOG.debug("2=="+loadedDomain);
        }
        domainSession.getTransaction().commit();
        domainSession.clear();
        errorCounter=0;
      }
      catch (Exception e) { 
        LOG.error("insert "+err,e); errorCounter++; domainSession.getTransaction().rollback();
        
        for (Domain d:domains) LOG.error(d);
        
        continue;
      }
      finally { domainSession.close(); }
      break;
    }
  }
}

class InsertIPThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(InsertIPThread.class);
  private List<IPAddress> ipAddresses;

  public InsertIPThread(List<IPAddress> records) { this.ipAddresses=records; }

  @Override
  public void run() {
    while (true) {
      Session session = DBUtil.openSession();
      try { session.beginTransaction();
        for (IPAddress ip:ipAddresses) {
          LOG.debug(ip);
          if (ip.getId()==null) {
            ip.setId(((IPAddress)session.createSQLQuery(String.format(DB_INSERT_IP_TEMPLATE, ip.getIpAddress(), ip.getIpAddress(), ip.getIpAddress()))
                                          .addEntity(IPAddress.class).uniqueResult()).getId());
            LOG.debug("saved:"+ip);
          }
        }
        session.getTransaction().commit();
        session.clear();
        LOG.debug("*  IP  STOP *******************************************************************");
        break;
      }
      catch (Exception e) {
        LOG.error("ipTH1",e);
        session.getTransaction().rollback();
      }
      finally { session.close(); }
    }
  }
}

class InsertDNSThread implements Runnable {
  Logger LOG=Logger.getLogger(InsertDNSThread.class);
  private List<DNSServer> dnsServers;
  public InsertDNSThread(List<DNSServer> records) { this.dnsServers=records; }
  @Override
  public void run() {
    while (true) {
      Session session = DBUtil.openSession();
      try { session.beginTransaction();
        for (DNSServer dns:dnsServers) {
          LOG.debug(dns);
          if (dns.getId()==null) {
            DNSServer loadedDns=(DNSServer)session.createSQLQuery(String.format(DB_INSERT_DNS_TEMPLATE, dns.getDomainName(), dns.getDomainName(), dns.getDomainName()))
                .addEntity(DNSServer.class).uniqueResult();
            dns.setId(loadedDns.getId());
            LOG.debug("saved:"+dns);
          }
        }
        session.getTransaction().commit();
        session.clear();
        LOG.debug("*  DNS STOP *******************************************************************");
        break;
      }
      catch (Exception e) { LOG.error("DNS",e); session.getTransaction().rollback(); }
      finally { session.close(); }
    }
  }
}