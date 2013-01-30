package com.gmsxo.domains.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import com.gmsxo.domains.data.DNSServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IPAddress;
import com.gmsxo.domains.db.DBUtil;

public class InsertDomains {
  private static Logger LOG=Logger.getLogger(InsertDomains.class);
  public static void main(String[] args) {
    try {
      new InsertDomains().doJob();
    } catch (IOException e) {
      LOG.error("main",e);
    }
    finally {
      DBUtil.close();
    }
  }

  public InsertDomains() {}
  
  public void doJob() throws IOException {
    String fileName="c:\\temp\\domains\\full-zone.biz.ks392bNq9dt.001.cut.res";
    List<Domain> domains=new LinkedList<>();
    List<IPAddress> ipAddresses=new LinkedList<>();
    List<DNSServer> dnsServers=new LinkedList<>();
    long counter=0l;
    String insertIPTemplate="with insert_ip_check as ("+
        "insert into IP_ADDRESS (\"ip_address\") "+
        "select '%s' "+
        "where not exists (select id from IP_ADDRESS where IP_ADDRESS='%s') "+
        "returning ip_address.id, ip_address.ip_address"+
      ") "+
      "select id,ip_address from insert_ip_check "+ 
      "union select id,ip_address from ip_address where ip_address='%s'";
    
    String insertDNSTemplate="with insert_dns_check as ("+
        "insert into DNS_SERVER (\"domain_name\") "+
        "select '%s' "+
        "where not exists (select id from DNS_SERVER where DOMAIN_NAME='%s') "+
        "returning dns_server.id, dns_server.domain_name, dns_server.ip_address_id"+
      ") "+
      "select id,domain_name,null as ip_address, ip_address_id from insert_dns_check "+ 
      "union select id,domain_name,null as ip_address, ip_address_id from dns_server where domain_name='%s'";
    long time=new Date().getTime();
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(fileName),StandardCharsets.UTF_8)) {
      
      while (true) {
        counter++;

        String line=reader.readLine();
        if (line==null) break;
        if (counter<0) continue;
        String[] splitLine=line.split(" ",-1);
        if (splitLine.length<3) continue;
        
        Domain domain = new Domain(splitLine[0]);
        IPAddress ipAddress = new IPAddress(splitLine[1]);
        
        for (int i=2;i<splitLine.length;i++) {
          if (splitLine[i].equals("ns.biz")) continue;
          DNSServer dnsServer=new DNSServer(splitLine[i]);
          dnsServers.add(dnsServer);
          domain.addDnsServer(dnsServer);
        }
        
        domain.setIPAddress(ipAddress);
        domains.add(domain);
        ipAddresses.add(ipAddress);
        LOG.info(counter+" "+domain);
        if (counter%100==0) {
          Session session = DBUtil.openSession();
          session.beginTransaction();
          //LOG.info(insert.toString());
          for (IPAddress ip:ipAddresses) {
            SQLQuery query = session.createSQLQuery(String.format(insertIPTemplate, ip.getIpAddress(), ip.getIpAddress(), ip.getIpAddress())).addEntity(IPAddress.class);
            IPAddress insertedIP = (IPAddress)query.uniqueResult();
            LOG.debug(insertedIP);
            ip.setId(insertedIP.getId());
          }
          ipAddresses.clear();
          
          for (DNSServer dns:dnsServers) {
            SQLQuery query = session.createSQLQuery(String.format(insertDNSTemplate, dns.getDomainName(), dns.getDomainName(), dns.getDomainName())).addEntity(DNSServer.class);
            DNSServer dnsServer = (DNSServer)query.uniqueResult();
            LOG.debug(dnsServer);
            dns.setId(dnsServer.getId());
          }
          
          dnsServers.clear();
          
          session.getTransaction().commit();
          session.clear();
          session.close();
          
          Session domainSession = DBUtil.openSession();
          
          domainSession.beginTransaction();
          
          for (Domain d:domains) {
            boolean save=false;
            LOG.debug("1=="+d);
            Domain loadedDomain=(Domain)domainSession.createQuery("from Domain d where d.domainName=:domainName")
                                                     .setParameter("domainName", d.getDomainName())
                                                     .uniqueResult();
            if (loadedDomain==null) {
              domainSession.save(d);
            }
            else {
              if (loadedDomain.getIpAddress().getId()!=d.getIpAddress().getId()) {
                loadedDomain.setIPAddress(d.getIpAddress());
                save=true;
              }
              if (loadedDomain.getDnsServer().size()!=d.getDnsServer().size()) {
                save=true;
                loadedDomain.setDnsServer(d.getDnsServer());
              }
              else {
                nextDNS: for (DNSServer loadedDNS:loadedDomain.getDnsServer()) {
                  for (DNSServer dns:d.getDnsServer()) {
                    if (loadedDNS.getId()==dns.getId()) continue nextDNS;
                  }
                  save=true;
                  loadedDomain.setDnsServer(d.getDnsServer());
                  LOG.warn("Not found: " + loadedDNS);
                }
              }
            }
            LOG.debug("0=="+loadedDomain);
            if (save) domainSession.save(loadedDomain); 
            LOG.debug("1=="+d);
            LOG.debug("2=="+loadedDomain);
          }
          
          domainSession.getTransaction().commit();
          domainSession.clear();
          domainSession.close();
          domains.clear();
          //break;
        }
      }
    }
    LOG.info("Time: "+(new Date().getTime()-time)/1000l);
  }
}
