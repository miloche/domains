package com.gmsxo.domains.imports.v3;

import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.helpers.DNSHelper;

public class TryThis {
  private static final Logger LOG=Logger.getLogger(TryThis.class);
  static TreeSet<IpAddress> ips=new TreeSet<>();
  static {
    ips.add(new IpAddress("1"));
    ips.add(new IpAddress("2"));
    ips.add(new IpAddress("3"));
    ips.add(new IpAddress("4"));
    ips.add(new IpAddress("5"));
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    //String regex="([a-z0-9][a-z0-9-]+\\.)*[a-z0-9-]+\\.[a-z]+";
    //String regex="^[a-z0-9]+([\\-\\.]{1}[a-z0-9\\-]+)*\\.[a-z]{2,5}$";
    String regex="^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$";
    //String regex="^[a-z0-9]+([\\-\\.]{1}[a-z0-9\\-]+)*\\.[a-z]{2,5}$";
    String dnsServerDomainName="dns249.d.register.com";
    boolean is=dnsServerDomainName.matches(DNSHelper.DNS_CHECK_REGEX);
    //boolean is=dnsServerDomainName.matches(regex);
    System.out.println(is);
  }
}
