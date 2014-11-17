package com.gmsxo.domains.resolve.v1;

import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.dns.DNSLookup;
import com.gmsxo.domains.helpers.DNSHelper;

/**
 * A class to resolve the IP address for a given domain name by calling NS lookup of a given DNS server.
 * 
 * @author miloxe
 *
 */
public class NSLookupThread implements Callable<NSLookupThread.NSLookupResult> {
  private static final Logger LOG = Logger.getLogger(NSLookupThread.class);
  private Domain domain;
  private int    timeout;
  private Attributes attrs;
  
  public NSLookupThread(Domain domain, int timeout) {this.domain=domain; this.timeout=timeout;}

  /**
   * Calls NS lookup for a given domain and DNS servers to resolve its IP address.
   * It updates the IP address of the given domain object and set the error attribute of the result object if the result of the call was error.
   * 
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public NSLookupResult call() {
    try {
      LOG.debug("Thr.st:"+domain);
      if (domain==null) return new NSLookupResult(domain,true);
      boolean wasError=false;
      try {
        attrs=DNSLookup.nsLookUp(domain.getName(), domain.getDnsServer(), timeout);
        wasError=!parseAttributes();
        LOG.trace("Thr.:"+wasError+" "+domain+" "+domain.getIpAddress()+" "+domain.getDnsServer());
      } catch (NamingException e) {
        LOG.debug(domain+"/"+e.getExplanation()+"/"+e.getResolvedName()+"/"+e.getRemainingName()+"/"+e.getMessage());
        for(Entry<String, String> entry:IpAddress.errorMap.entrySet())
          if (e.getExplanation()!=null&&e.getExplanation().contains(entry.getKey())) domain.setIpAddress(new IpAddress(entry.getValue()));
        if (domain.getIpAddress()==null) domain.setIpAddress(new IpAddress("ERROR"));
        wasError=true;
      }
      catch (Exception e) { LOG.info(domain+"/"+e.getMessage(),e); wasError=true;}
      return new NSLookupResult(domain,wasError);
    }
    finally { LOG.debug("Thr.en:"+domain); }
  }
  /**
   * Parse returned lookup attributes and extract the domain from them.
   * 
   * @param domain
   * @param attrs
   * @return
   * @throws NamingException
   */
  private boolean parseAttributes() throws NamingException {
    boolean wasIpSet=true;
    if (attrs!=null) {
      extractDns();
      wasIpSet=extractIp();
    }
    LOG.trace(domain+" "+domain.getIpAddress());
    if (domain.getIpAddress()==null||domain.getIpAddress().getAddress()==null) {
      LOG.trace("NULL_IP");
      domain.setIpAddress(new IpAddress("NULL_IP"));
      wasIpSet=false;
    }
    return wasIpSet;
  }
  /**
   * Extract DNS servers from lookup attributes and add them to domain.
   * 
   * @param domain
   * @param attrs
   * @throws NamingException
   */
  private void extractDns() throws NamingException {
    Attribute attrNS = attrs.get("NS");
    if (attrNS!=null) {
      NamingEnumeration<?> allNS = attrNS.getAll();
      if (allNS!=null) next: while(allNS.hasMoreElements()) { // go through all DNS servers
        String dnsDomainName = DNSLookup.removeDot(allNS.next().toString().toLowerCase()).trim(); // remove the dot at the end of the name
        if (!dnsDomainName.matches(DNSHelper.DNS_CHECK_REGEX) && !dnsDomainName.matches(DNSHelper.IP_CHECK_REGEXP)) continue next;
        for (DnsServer dns:domain.getDnsServer()) if (dns.getName().equals(dnsDomainName)) continue next;
        domain.addDnsServer(new DnsServer(dnsDomainName));
      }
    }
  }
  /**
   * Parse IP address from lookup attributes and set it to domain.
   * 
   * @param domain
   * @param attrs
   * @return
   * @throws NamingException
   */
  private boolean extractIp() throws NamingException {
    Attribute attrA = attrs.get("A");
    if (attrA!=null) {
      String ip=(String)attrA.get();
      LOG.debug("ip:"+ip);
      if (ip!=null) domain.setIpAddress(new IpAddress(ip.trim()));
      else domain.setIpAddress(new IpAddress());
      return true;
    }
    else { return false;}
  }
  /**
   * The thread result class.
   * 
   * @author miloxe
   *
   */
  public static class NSLookupResult {
    private Domain domain;
    private boolean isError;
    public NSLookupResult(Domain domain){ this.domain=domain; } public NSLookupResult(Domain domain,boolean error){this.domain=domain; this.isError=error;}
    public Domain getDomain(){return domain;}
    public boolean getIsError() {return isError;}
    @Override public String toString() { return new StringBuilder("Result [domain=").append(domain).append(", isError=").append(isError).append("]").toString(); }
  }
}
