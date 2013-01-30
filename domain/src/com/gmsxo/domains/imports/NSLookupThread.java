package com.gmsxo.domains.imports;

import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DNSServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IPAddress;
import com.gmsxo.domains.dns.DNSLookup;

public class NSLookupThread implements Callable<NSLookupThread.Result> {
  private static final Logger LOG = Logger.getLogger(NSLookupThread.class);
  public static String topLevel;
  private Domain domain;
  
  public NSLookupThread(Domain domain) { this.domain=domain; }
  
  private void parseAttributes(Domain domain, Attributes attrs) throws NamingException {
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
  public Result call() throws Exception {
    if (domain==null) return new Result(domain,true);
    boolean wasError=false;
    try {
      parseAttributes(domain, DNSLookup.nsLookUp(domain.getDomainName(), domain.getDnsServer().get(0).getDomainName()));
    } catch (NamingException e) {
      LOG.debug(domain+"/"+e.getExplanation()+"/"+e.getResolvedName()+"/"+e.getRemainingName()+"/"+e.getMessage());
      for(Entry<String, String> entry:IPAddress.errorMap.entrySet()) if (e.getExplanation().contains(entry.getKey())) domain.setIPAddress(new IPAddress(entry.getValue()));
      if (domain.getIpAddress()==null) domain.setIPAddress(new IPAddress("ERROR"));
      wasError=true;
    }
    catch (Exception e) { LOG.info(domain+"/"+e.getMessage(),e); wasError=true;}
    return new Result(domain,wasError);
  }

  public class Result {
    private Domain domain;
    private boolean error;
    public Result(){} public Result(Domain domain,boolean error){this.domain=domain; this.error=error;}
    public Domain getDomain(){return domain;}
    public boolean getError() {return error;}
  }
}
