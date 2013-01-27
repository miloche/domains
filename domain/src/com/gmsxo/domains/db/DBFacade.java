package com.gmsxo.domains.db;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

import com.gmsxo.domains.data.DNSServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IPAddress;

public final class DBFacade {
  private static final Logger LOG = Logger.getLogger(DBFacade.class);
  public static volatile boolean initialized=false;
  private static IPAddress NULLIP;
  static List<IPAddress> errors=new LinkedList<>();
  
  public static List<IPAddress> getErrors() {return errors;}
  static void setNullIP(IPAddress nullIP) {
    NULLIP=nullIP;
  }
  
  public static IPAddress getNullIP() {
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
  
  public static IPAddress saveOrUpdate(IPAddress ipAddress) {
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
        query.setString("key", ipAddress.getIpAddress());
        return (IPAddress)query.uniqueResult();
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
  
  public static DNSServer saveOrUpdate(DNSServer dnsServer) {
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
        query.setString("key", dnsServer.getDomainName());
        return (DNSServer)query.uniqueResult();
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
  
  public static List<DNSServer> saveOrUpadteDNSServer(List<DNSServer> dnsServers) {
    for (int index=0;index<dnsServers.size();index++) {
      dnsServers.set(index, saveOrUpdate(dnsServers.get(index)));
    }
    return dnsServers;
  }
   

  
  public static Domain saveOrUpdateDomain(Domain domain) {
    //LOG.info("saveOrUpdateDomain "+domain);
    saveOrUpadteDNSServer(domain.getDnsServer());
    if (domain.getIpAddress().getId()==null) domain.setIPAddress(saveOrUpdate(domain.getIpAddress()));
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
        query.setString("key", domain.getDomainName());
        Domain savedDomain = (Domain)query.uniqueResult();
        savedDomain.setIPAddress(domain.getIpAddress());
        savedDomain.setDnsServer(domain.getDnsServer());
        session1.save(savedDomain);
        return savedDomain;
      }
      finally {
        //session1.flush();
        session1.close();
      }
    }
    finally {
      if (session!=null) {
        //session.flush();
        session.close();
      }
      LOG.info("saveOrUpdate: saved: " + domain);
    }
    
    return domain;
  }
}
