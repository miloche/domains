package com.gmsxo.domains;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Context;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DBFacade;
import com.gmsxo.domains.data.DNSLookUpJob;
import com.gmsxo.domains.data.DNSServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IPAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class DNSLookUPResult {
  private List<Domain> domainList;
  private long done;

  public DNSLookUPResult(List<Domain> domainList, long done) {
    super();
    this.domainList = domainList;
    this.done = done;
  }
  public List<Domain> getDomainList() {
    return domainList;
  }
  public void setDomainList(List<Domain> domainList) {
    this.domainList = domainList;
  }
  public long getDone() {
    return done;
  }
  public void setDone(long done) {
    this.done = done;
  }
  
}

class DNSLookUPThread implements Callable<DNSLookUPResult> {
  private static final Logger LOG = Logger.getLogger(DNSLookUPThread.class);
  private static final IPAddress IPADDRESS_NONE = new IPAddress(0, "NONE");
  
  private DNSLookUpJob dnsLookUpJob;
  private List<Domain> domainList;
  private String dns;

  public DNSLookUPThread(String dns) {
    this.dns = dns;
  }
  public DNSLookUPThread(List<Domain> domainList, String dns) {
    this(dns);
    this.domainList = domainList;
  }

  @Override
  public DNSLookUPResult call() throws Exception {
    
    dnsLookUpJob = new DNSLookUpJob();
    DBFacade.insertDnsLookupJob(dnsLookUpJob);
    //LOG.debug(dnsLookUpJob);

    if (domainList == null) domainList = DBFacade.getDomais(dnsLookUpJob.getId());
    for (Domain domain : domainList)
    try {
      try {
        Attributes attrs = DNSLookup.nsLookUp(domain.getDomainName(), dns);
        Attribute ip = attrs.get("A");
        if (ip!=null) {
          IPAddress ipAddress = new IPAddress((String) ip.get());
          try {
            DBFacade.insertIPAddress(ipAddress);
          } catch (com.ibatis.common.jdbc.exception.NestedSQLException e) {
            if (e.getCause().getMessage().contains("duplicate key value")) {
              ipAddress.setId(DBFacade.getIPAddressId(ipAddress.getIpAddress()));
            } else throw e;
          }
          domain.setIPAddress(ipAddress);
        }
        else {
          throw new javax.naming.NameNotFoundException();
        }
      } catch (javax.naming.NameNotFoundException e) {
        domain.setIPAddress(IPADDRESS_NONE);
      }
      if (domain.getIpAddressId() != null) {
        DBFacade.updateDomainIpAddressId(domain);
      }
    } catch (Exception e) {
      LOG.error(">>unexpected exception" + e);
      Thread.sleep(1000);
    }
    
    int all = domainList.size();
    int done = 0;

    Iterator<Domain> domainIter = domainList.listIterator();
    while (domainIter.hasNext()) {
      Domain domain = domainIter.next();
      //LOG.debug(domain);
      if (domain.getIpAddressId() != null) {
        done++;
        domainIter.remove();
      }
    }
    
    LOG.info("C: " + DNSLookup.counter + " All: " + all + " Done: " + done + " " + dns);
    if (domainList == null || domainList.size() == 0) return new DNSLookUPResult(null, done);
    return new DNSLookUPResult(domainList, done);
  }
}

public class DNSLookup {
  private static final Logger LOG = Logger.getLogger(DNSLookup.class);
  private int lastDNSServerId = -1;
  public static volatile int counter=0;
  
  private DNSServer getNextDNSServer() throws SQLException {
    DNSServer dnsServer = DBFacade.getNextDNSServer(lastDNSServerId);
    if (dnsServer == null) {
      lastDNSServerId = -1;
      dnsServer = DBFacade.getNextDNSServer(lastDNSServerId);
    }
    lastDNSServerId++;
    return dnsServer;
  }

  public boolean doJob() throws SQLException {
    final int POOL_SIZE = 500;
    long start = new Date().getTime();
    
    ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
    
    List<Future<DNSLookUPResult>> list = new ArrayList<>();
    for (int i=0; i<POOL_SIZE; i++) {
      Callable<DNSLookUPResult> worker = new DNSLookUPThread(getNextDNSServer().getIpAddress());
      Future<DNSLookUPResult> submit = pool.submit(worker);
      list.add(submit);
      counter+=10;
    }
    
    try {
      int i=0;
      while (true) {
        if (counter < 1000 && list.get(i).isDone()) {
          list.set(i++, pool.submit(new DNSLookUPThread(null, getNextDNSServer().getIpAddress())));
          counter += 10;
          if (i==list.size()) i=0;
        }
        if (counter>=1000) break;
      }
    }
    catch (Exception e) {
      LOG.error("Unexpected excpetion", e);
    }
    finally {
      pool.shutdown();
    }
    while (!pool.isTerminated());
    long totalTime = (new Date().getTime() - start)/1000;
    LOG.debug("Done: " + counter + " total time: " + totalTime + " per sec: " + counter/totalTime) ;
    return true;
  }

  public static void main(String args[]) throws NamingException, SQLException {
    while (new DNSLookup().doJob())
      break;
    //nsLookUp1("yveslefranc.com");
  }

  public static Attributes reverseNsLookup(String ip, String dns) throws NamingException {
    final String[] bytes = ip.split("\\.");
    final String reverseDnsDomain = bytes[3] + "." + bytes[2] + "." + bytes[1] + "." + bytes[0] + ".in-addr.arpa";
    return nsLookUp(reverseDnsDomain, dns);
  }

  public static Attributes nsLookUp(String domainName, String dns) throws NamingException {
    //LOG.info("nsLookUp: " + domainName + " dns: " + dns);
    Hashtable<String, Object> env = new Hashtable<String, Object>();
    env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
    env.put("java.naming.provider.url", "dns://" + dns);
    env.put("com.sun.jndi.dns.recursion", "true");
    //env.put(Context.AUTHORITATIVE, "true");

    DirContext ictx = new InitialDirContext(env);
    return ictx.getAttributes(domainName, new String[]{"A", "AAAA", "NS", "CNAME", "SOA", "PTR", "MX", "TXT", "HINFO", "NAPTR", "SRV"});
  }

  public static void nsLookUp1(String domainName) {
    try {
      InetAddress inetAddress;
      // if first character is a digit then assume is an address
      if (Character.isDigit(domainName.charAt(0))) {   // convert address from
                                                     // string
        // representation to byte
        // array
        byte[] b = new byte[4];
        String[] bytes = domainName.split("[.]");
        for (int i = 0; i < bytes.length; i++) {
          b[i] = new Integer(bytes[i]).byteValue();
        }
        // get Internet Address of this host address
        inetAddress = InetAddress.getByAddress(b);
      } else {   // get Internet Address of this host name
        inetAddress = InetAddress.getByName(domainName);
      }
      // show the Internet Address as name/address
      System.out.println(inetAddress.getHostName() + "/" + inetAddress.getHostAddress());
      // get the default initial Directory Context
      InitialDirContext iDirC = new InitialDirContext();
      // get the DNS records for inetAddress
      Attributes attributes = iDirC.getAttributes("dns:/" + inetAddress.getHostName());
      // get an enumeration of the attributes and print them out
      NamingEnumeration<? extends Attribute> attributeEnumeration = attributes.getAll();
      System.out.println("-- DNS INFORMATION --");
      while (attributeEnumeration.hasMore()) {
        System.out.println("" + attributeEnumeration.next());
      }
      attributeEnumeration.close();
    } catch (UnknownHostException exception) {
      System.err.println("ERROR: No Internet Address for '" + domainName + "'");
    } catch (NamingException exception) {
      System.err.println("ERROR: No DNS record for '" + domainName + "'");
    }
  }
  //private void sleep(long time) {try {Thread.sleep(3000);} catch (InterruptedException e) {LOG.error("Sleep interrupted",e);}}
}
