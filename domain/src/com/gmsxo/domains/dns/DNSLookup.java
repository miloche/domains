package com.gmsxo.domains.dns;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DNSServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IPAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public final class DNSLookup { private DNSLookup() {}
private static Logger LOG = Logger.getLogger(DNSLookup.class);

private static void parseAttributes(Domain domain, Attributes attrs) throws NamingException {
  LOG.debug("4 "+domain);
  if (attrs!=null) {
    Attribute attrNS = attrs.get("NS");
    if (attrNS!=null) {
      NamingEnumeration<?> allNS = attrNS.getAll();
      if (allNS!=null) next: while(allNS.hasMoreElements()) {
        String dnsRaw=allNS.next().toString();
        String dnsDomainName = DNSLookup.removeDot(dnsRaw.toLowerCase()).trim();
        for (DNSServer dns:domain.getDnsServer()) {
          if (dns.getDomainName().equals(dnsDomainName)) {
            continue next;
          }
        }
        domain.addDnsServer(new DNSServer(dnsDomainName));
      }
    }

    Attribute attrA = attrs.get("A");
    if (attrA!=null) domain.setIPAddress(new IPAddress((String)attrA.get()));
  }
  if (domain.getIpAddress()==null||domain.getIpAddress().getIpAddress()==null) domain.setIPAddress(new IPAddress("NULL_IP"));
}

public static void main(String args[]) throws NamingException {
  List<DNSServer> dns = new LinkedList<DNSServer>();
  dns.add(new DNSServer("ns1.hoangthanh.net"));
  dns.add(new DNSServer("ns2.hoangthanh.net"));
  Domain domain=new Domain("doanhnhantrebinhdinh.com");
  domain.setDnsServer(dns);
  Attributes attrs = nsLookUp(domain.getDomainName(), domain.getDnsServer().get(0).getDomainName(), 1000);
  
  if (attrs!=null) {
    Attribute attrNS = attrs.get("NS");
    if (attrNS!=null) {
      NamingEnumeration<?> allNS = attrNS.getAll();
      if (allNS!=null) while(allNS.hasMoreElements()) LOG.info(new DNSServer(DNSLookup.removeDot(allNS.next().toString())));
    }

    Attribute attrA = attrs.get("A");
    if (attrA!=null) LOG.info(new IPAddress(((String)attrA.get()).trim()));
  }
  
  parseAttributes(domain, attrs);
  LOG.debug(domain);
}
  //private static final Logger LOG=Logger.getLogger(DNSLookup.class);
  //private static final String GOOGLE_DNS="dns://google-public-dns-a.google.com dns://google-public-dns-b.google.com";
  
  public static String removeDot(String rawDomain) {
    if (rawDomain==null || rawDomain.length()==0) return rawDomain;
    return rawDomain.substring(0, rawDomain.length()-1);
  }
  
  public static String formatDomain(String rawDomain, String topLevel) {
    if (rawDomain.substring(rawDomain.length()-1,rawDomain.length()).equals(".")) return removeDot(rawDomain).toLowerCase();
    return rawDomain.toLowerCase() + topLevel;
  }
  public static String formatDNS(String rawDNS, String topLevel) {
    String dns;
    if (rawDNS.charAt(rawDNS.length()-1) == '.') {
      dns = rawDNS.substring(0, rawDNS.length()-1).toLowerCase();
    }
    else {
      dns = rawDNS.toLowerCase() + topLevel;
    }
    return dns;
  }


  public static Attributes reverseNsLookup(String ip, String dns, int timeout) throws NamingException {
    final String[] bytes = ip.split("\\.");
    final String reverseDnsDomain = bytes[3] + "." + bytes[2] + "." + bytes[1] + "." + bytes[0] + ".in-addr.arpa";
    return nsLookUp(reverseDnsDomain, dns, timeout);
  }
  
  public static Attributes nsLookUp(String domainName, String dns, int timeout) throws NamingException {
    DNSServer dnsServer = new DNSServer(dns);
    List<DNSServer> dnsServers = new LinkedList<>();
    dnsServers.add(dnsServer);
    return nsLookUp(domainName, dnsServers, timeout);
  }

  public static Attributes nsLookUp(String domainName, List<DNSServer> dns, int timeout) throws NamingException {
    //LOG.info("nsLookUp: " + domainName + " dns: " + dns);
    Hashtable<String, Object> env = new Hashtable<String, Object>();
    env.put("java.naming.factory.initial", "com.gmsxo.domains.dns.dnsclient.DnsContextFactory");
    //env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
    
    StringBuilder dnsServers = new StringBuilder("");        
    for( DNSServer dnsServer : dns ) dnsServers.append("dns://").append(dnsServer.getDomainName()).append(" ");
    //LOG.debug(dnsServers.toString());
    env.put("java.naming.provider.url", dnsServers.toString());
    //env.put("java.naming.provider.url", GOOGLE_DNS);
    
    //env.put("com.sun.jndi.dns.recursion", "true");
    env.put("com.sun.jndi.dns.timeout.initial", ""+timeout);
    env.put("com.sun.jndi.dns.timeout.retries", "1");
    LOG.debug("5 "+domainName);
    DirContext ictx = new InitialDirContext(env);
    LOG.debug("6 "+domainName);
    //return ictx.getAttributes(domainName, new String[]{"A", "AAAA", "NS", "CNAME", "SOA", "PTR", "MX", "TXT", "HINFO", "NAPTR", "SRV"});
    return ictx.getAttributes(domainName, new String[]{"A", "NS"});
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
