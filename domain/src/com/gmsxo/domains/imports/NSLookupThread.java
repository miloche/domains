package com.gmsxo.domains.imports;

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
        if (allNS!=null) while(allNS.hasMoreElements()) domain.getDnsServer().add(new DNSServer(DNSLookup.removeDot(allNS.next().toString())));
      }
  
      Attribute attrA = attrs.get("A");
      if (attrA!=null) domain.setIPAddress(new IPAddress((String)attrA.get()));
    }
    //if (domain.getIpAddress()==null) domain.setIPAddress(DBFacade.getNullIP());
    if (domain.getIpAddress()==null||domain.getIpAddress().getIpAddress()==null) domain.setIPAddress(new IPAddress("NULL"));
  }
  
  @Override
  public Result call() throws Exception {
    if (domain==null) return new Result(domain);
    try {
      parseAttributes(domain, DNSLookup.nsLookUp(domain.getDomainName(), domain.getDnsServer()));
    } catch (NamingException e) {
      LOG.info(domain+"/"+e.getExplanation()+"/"+e.getResolvedName()+"/"+e.getRemainingName()+"/"+e.getMessage());
      //for(IPAddress error:DBFacade.getErrors()) if (e.getExplanation().contains(error.getIpAddress())) domain.setIPAddress(error);
      domain.setIPAddress(new IPAddress("ERROR"));
    }
    catch (Exception e) { LOG.info(domain+"/"+e.getMessage(),e); }
    return new Result(domain);
  }

  public class Result {
    private Domain domain;
    public Result(){} public Result(Domain domain){this.domain=domain;} public Domain getDomain(){return domain;}
  }
}
