package com.gmsxo.domains.data;

import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.dns.DNSLookup;

public class AReadIP {
  private static final Logger LOG = Logger.getLogger(AReadIP.class);

  /**
   * @param args
   * @throws NamingException 
   */
  public static void main(String[] args) throws NamingException {
    //Session session = DBUtil.getSessionFactory().openSession();
    //try {
      //session.beginTransaction();
      //IPAddress ip = (IPAddress)session.get(IPAddress.class, 131l);
      /*LOG.debug(ip);
      for (Domain domain:ip.getDomain()) LOG.debug(" "+domain);
      LOG.debug("Count: "+ip.getDomain().size());
      
      DNSServer dnsServer = (DNSServer)session.get(DNSServer.class, 50l);
      LOG.debug(dnsServer);
      
      for (Domain domain:dnsServer.getDomain())
        LOG.debug(domain);
      LOG.debug("Count: "+dnsServer.getDomain().size());
      */
    //}
    //finally {
      //session.getTransaction().commit();
      //session.close();
    //}
    //DBUtil.getSessionFactory().close();
    
    StringBuilder dnsServers = new StringBuilder("");        
    List nameservers = sun.net.dns.ResolverConfiguration.open().nameservers();
    for( Object dns : nameservers ) 
    {
        dnsServers.append("dns://").append(dns).append(" ");
    }
    LOG.info(dnsServers.toString());
    
    DNSLookup.nsLookUp1("BUSINESSMERIT.COM");
    LOG.info("-----");
    
    
    Attributes attrs = DNSLookup.nsLookUp("BUSINESSMERIT.COM", "8.8.4.4",1000);
    NamingEnumeration<? extends Attribute> enumAttrs = attrs.getAll();
    while(enumAttrs.hasMoreElements()) {
      Attribute attr = enumAttrs.next();
      LOG.debug("  " + attr.getID() + " = " + attr.get());
      NamingEnumeration<?> ne= attr.getAll();
      while (ne.hasMoreElements()) {
        Object next=ne.next();
        LOG.debug("  "+next.getClass()+" "+next);
      }
    }
  }

}
