package com.gmsxo.domains.imports;

import java.util.Map.Entry;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DNSServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IPAddress;
import com.gmsxo.domains.dns.DNSLookup;

public class NSLookupThreadRunnable implements Runnable {
  private static final Logger LOG = Logger.getLogger(NSLookupThreadRunnable.class);
  public static String topLevel;
  private Domain domain;
  private Result result;
  private int timeout;
  public NSLookupThreadRunnable(Domain domain, Result result, int timeout) { this.domain=domain; this.result=result; this.timeout=timeout; }
  
  private void parseAttributes(Domain domain, Attributes attrs) throws NamingException {
    LOG.debug("4 "+domain);
    if (attrs!=null) {
      Attribute attrNS = attrs.get("NS");
      if (attrNS!=null) {
        NamingEnumeration<?> allNS = attrNS.getAll();
        if (allNS!=null) next: while(allNS.hasMoreElements()) {
          String dnsDomainName = DNSLookup.removeDot(allNS.next().toString().toLowerCase());
          for (DNSServer dns:domain.getDnsServer()) if (dns.getDomainName().equals(dnsDomainName)) continue next;
          domain.addDnsServer(new DNSServer(dnsDomainName));
        }
      }
  
      Attribute attrA = attrs.get("A");
      if (attrA!=null) domain.setIPAddress(new IPAddress((String)attrA.get()));
    }
    if (domain.getIpAddress()==null||domain.getIpAddress().getIpAddress()==null) domain.setIPAddress(new IPAddress("NULL_IP"));
  }
  
  @Override
  public void run()  {
    LOG.debug("Thr.:"+domain);
    if (domain==null) {
      result.error=true;
      return;
    }
    boolean wasError=false;
    try {
      parseAttributes(domain, DNSLookup.nsLookUp(domain.getDomainName(), domain.getDnsServer().get(0).getDomainName(), timeout));
      LOG.debug("1 "+domain);
    } catch (NamingException e) {
      LOG.debug(domain+"/"+e.getExplanation()+"/"+e.getResolvedName()+"/"+e.getRemainingName()+"/"+e.getMessage());
      for(Entry<String, String> entry:IPAddress.errorMap.entrySet()) if (e.getExplanation()!=null&&e.getExplanation().contains(entry.getKey())) domain.setIPAddress(new IPAddress(entry.getValue()));
      if (domain.getIpAddress()==null) domain.setIPAddress(new IPAddress("ERROR"));
      wasError=true;
      LOG.debug("2 "+domain);
    }
    catch (Exception e) { LOG.info(domain+"/"+e.getMessage(),e); wasError=true;}
    LOG.debug("3 "+domain);
    result.error=wasError;
  }

  public static class Result {
    private Domain domain;
    private boolean error;
    private boolean isDone;
    public Result(Domain domain){ this.domain=domain; } public Result(Domain domain,boolean error){this.domain=domain; this.error=error;}
    public Domain getDomain(){return domain;}
    public boolean getError() {return error;}
    synchronized void setIsDone(boolean isDone) {this.isDone=isDone;}
    synchronized boolean getIsDone() {return this.isDone;}
  }
}
