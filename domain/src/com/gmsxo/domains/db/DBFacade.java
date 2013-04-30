package com.gmsxo.domains.db;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;

public final class DBFacade {
  private static final Logger LOG = Logger.getLogger(DBFacade.class);
  public static volatile boolean initialized=false;
  private static IpAddress NULLIP;
  static List<IpAddress> errors=new LinkedList<>();
  
  public static List<IpAddress> getErrors() {return errors;}
  static void setNullIP(IpAddress nullIP) {
    NULLIP=nullIP;
  }
  
  public static IpAddress getNullIP() {
    return NULLIP;
  }
  
  static {
    if (!initialized) {
      LOG.info("db init started");
      try {
        Session session = DBUtil.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
        initialized=true;
        LOG.info("db init finished");
      }
      catch(Exception e) {
        System.out.println("ERR");
        e.printStackTrace();
        System.exit(100);
      }
    }
  }
  
  public static final String DB_INSERT_IP_TEMPLATE="with insert_ip_check as ("+
      "insert into IP_ADDRESS (\"ip_address\") "+
      "select '%s' "+
      "where not exists (select id from IP_ADDRESS where IP_ADDRESS='%s') "+
      "returning ip_address.id, ip_address.ip_address"+
    ") "+
    "select id,ip_address from insert_ip_check "+ 
    "union select id,ip_address from ip_address where ip_address='%s'";
  
  public static final String DB_INSERT_DNS_TEMPLATE="with insert_dns_check as ("+
      "insert into DNS_SERVER (\"domain_name\") "+
      "select '%s' "+
      "where not exists (select id from DNS_SERVER where DOMAIN_NAME='%s') "+
      "returning dns_server.id, dns_server.domain_name, dns_server.ip_address_id"+
    ") "+
    "select id,domain_name,null as ip_address, ip_address_id from insert_dns_check "+ 
    "union select id,domain_name,null as ip_address, ip_address_id from dns_server where domain_name='%s'";

  
  public static IpAddress saveOrUpdate(IpAddress ipAddress) {
    //LOG.info("saveOrUpdate: " + ipAddress);
    Session session = DBUtil.openSession();
    try {
      
      session.beginTransaction();
      session.save(ipAddress);
      session.getTransaction().commit();
      
    }
    catch (ConstraintViolationException e) {
      //LOG.info("saveOrUpdate: constraint: " + ipAddress);
      session.getTransaction().rollback();
      session.close();
      session=null;
        
      Session session1 = DBUtil.openSession();
      try {
        Query query = session1.createQuery("from IPAddress where ipAddress = :key");
        query.setString("key", ipAddress.getAddress());
        return (IpAddress)query.uniqueResult();
      }
      finally {
        session1.close();
      }
    }
    finally {
      if (session!=null) session.close();
    }
    //LOG.info("saveOrUpdate: saved: " + ipAddress);
    return ipAddress;
    
  }
  
  public static DnsServer saveOrUpdate(DnsServer dnsServer) {
    //LOG.info("saveOrUpdate: " + dnsServer);
    Session session = DBUtil.openSession();
    try {
      
      session.beginTransaction();
      session.save(dnsServer);
      session.getTransaction().commit();
      
    }
    catch (ConstraintViolationException e) {
      //LOG.info("saveOrUpdate: constraint: " + dnsServer);
      session.getTransaction().rollback();
      session.close();
      session=null;
        
      Session session1 = DBUtil.openSession();
      try {
        Query query = session1.createQuery("from DNSServer where domainName = :key");
        query.setString("key", dnsServer.getName());
        return (DnsServer)query.uniqueResult();
      }
      finally {
        session1.close();
      }
    }
    finally {
      if (session!=null) session.close();
    }
    //LOG.info("saveOrUpdate: saved: " + dnsServer);
    return dnsServer;
    
  }
  
  public static Domain saveOrUpadteDNSServer(Domain domain) {
    Set<DnsServer> updated = new TreeSet<>();
    
    for (DnsServer dns:domain.getDnsServer()) {
      updated.add(saveOrUpdate(dns));
    }
    domain.setDnsServer(updated);
    return domain;
  }
  
  public static Domain saveOrUpdateDomain(Domain domain) {
    //LOG.info("saveOrUpdateDomain "+domain);
    saveOrUpadteDNSServer(domain);
    if (domain.getIpAddress().getId()==null) domain.setIpAddress(saveOrUpdate(domain.getIpAddress()));
    Session session = DBUtil.openSession();
    try {
      session.beginTransaction();
      session.save(domain);
      session.getTransaction().commit();
    }  catch (ConstraintViolationException e) {
      //LOG.info("saveOrUpdate: constraint: " + domain);
      session.getTransaction().rollback();
      session.close();
      session=null;
        
      Session session1 = DBUtil.openSession();
      try {
        Query query = session1.createQuery("from Domain where domainName = :key");
        query.setString("key", domain.getName());
        Domain savedDomain = (Domain)query.uniqueResult();
        savedDomain.setIpAddress(domain.getIpAddress());
        savedDomain.setDnsServer(domain.getDnsServer());
        session1.save(savedDomain);
        return savedDomain;
      }
      finally {
        session1.close();
      }
    }
    finally {
      if (session!=null) {
        session.close();
      }
      LOG.info("saveOrUpdate: saved: " + domain);
    }
    
    return domain;
  }
}
