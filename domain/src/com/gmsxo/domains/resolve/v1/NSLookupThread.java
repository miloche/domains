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

public class NSLookupThread implements Callable<NSLookupThread.Result> {
  private static final Logger LOG = Logger.getLogger(NSLookupThread.class);
  public static String topLevel;
  private Domain domain;
  private int timeout;
  
  public NSLookupThread(Domain domain, int timeout) { this.domain=domain; this.timeout=timeout;}
  
  private boolean parseAttributes(Domain domain, Attributes attrs) throws NamingException {
    boolean wasIpSet=true;
    if (attrs!=null) {
      Attribute attrNS = attrs.get("NS");
      if (attrNS!=null) {
        NamingEnumeration<?> allNS = attrNS.getAll();
        if (allNS!=null) next: while(allNS.hasMoreElements()) {
          String dnsDomainName = DNSLookup.removeDot(allNS.next().toString().toLowerCase()).trim();
          if (!dnsDomainName.matches(DNSHelper.DNS_CHECK_REGEX) && !dnsDomainName.matches(DNSHelper.IP_CHECK_REGEXP)) {
            //LOG.warn("DNS doesn't match: "+dnsDomainName+" "+domain.getName());
            continue next;
          }
          for (DnsServer dns:domain.getDnsServer()) if (dns.getName().equals(dnsDomainName)) continue next;
          domain.addDnsServer(new DnsServer(dnsDomainName));
        }
      }
  
      Attribute attrA = attrs.get("A");
      if (attrA!=null) {
        String ip=(String)attrA.get();
        LOG.debug("ip:"+ip);
        if (ip!=null) domain.setIpAddress(new IpAddress(ip.trim()));
        else domain.setIpAddress(new IpAddress(ip));
      }
      else {
        wasIpSet=false;
      }
    }
    LOG.debug(domain+" "+domain.getIpAddress());
    if (domain.getIpAddress()==null||domain.getIpAddress().getAddress()==null) {
      LOG.debug("NULL_IP");
      domain.setIpAddress(new IpAddress("NULL_IP"));
      wasIpSet=false;
    }
    return wasIpSet;
  }
  
  @Override
  public Result call() throws Exception {
    LOG.debug("Thr.:"+domain);
    if (domain==null) return new Result(domain,true);
    boolean wasError=false;
    try {
      wasError=!parseAttributes(domain, DNSLookup.nsLookUp(domain.getName(), domain.getDnsServer(), timeout));
      LOG.debug("Thr.: wasError="+wasError);
      LOG.debug("Thr.:"+domain+" "+(domain==null?"null":domain.getIpAddress())+" "+(domain==null?"null":domain.getDnsServer()));
    } catch (NamingException e) {
      LOG.debug(domain+"/"+e.getExplanation()+"/"+e.getResolvedName()+"/"+e.getRemainingName()+"/"+e.getMessage());
      for(Entry<String, String> entry:IpAddress.errorMap.entrySet())
        if (e.getExplanation()!=null&&e.getExplanation().contains(entry.getKey())) domain.setIpAddress(new IpAddress(entry.getValue()));
      if (domain.getIpAddress()==null) domain.setIpAddress(new IpAddress("ERROR"));
      wasError=true;
    }
    catch (Exception e) { LOG.info(domain+"/"+e.getMessage(),e); wasError=true;}
    return new Result(domain,wasError);
  }

  public static class Result {
    private Domain domain;
    private boolean isError;
    public Result(Domain domain){ this.domain=domain; } public Result(Domain domain,boolean error){this.domain=domain; this.isError=error;}
    public Domain getDomain(){return domain;}
    public boolean getIsError() {return isError;}
    @Override public String toString() { return new StringBuilder("Result [domain=").append(domain).append(", isError=").append(isError).append("]").toString(); }
  }
}
