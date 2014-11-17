package com.gmsxo.domains.helpers;

import java.util.Map;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;

public final class ImportHelper { private ImportHelper() {}
  public static final int IP_ADDRESS_LENGTH=15;

  
  public static final String[] pref = new String[] {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","digit","other"};
  //public static final String[] pref = new String[] {"h"};

  public static int getCharIndex(String name) {
    String firstChar = name.substring(0, 1);
    for (int i=0;i<pref.length-2;i++) if (firstChar.equals(pref[i])) return i;
    if (name.charAt(0)>='0' && name.charAt(0)<='9') return pref.length-2;
    return pref.length-1;
  }
  public static String getDomPartSuff(String name) { return pref[getCharIndex(name)]; }

  public static Domain parseResInputLine(String line, Map<String, IpAddress> ipAddressMap, Map<String, DnsServer> dnsServerMap) {

    if (line==null) return null;

    String[] splitLine=line.split(" ",-1);
    if (splitLine.length<3) return null;

    Domain domain = new Domain(splitLine[0]);
    String ip=splitLine[1].substring(0,Math.min(IP_ADDRESS_LENGTH, splitLine[1].length()));

    IpAddress ipAddress = null;
  
    if (ipAddressMap!=null) {
      ipAddress=ipAddressMap.get(ip);
      if (ipAddress==null) ipAddressMap.put(ip, ipAddress=new IpAddress(ip));
    }
    else {
      ipAddress=new IpAddress(ip);
    }

    domain.setIpAddress(ipAddress);
    
    nextdnsserver: for (int i=2;i<splitLine.length;i++) {
      String dnsServerDomainName=splitLine[i];

      // this line can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
      if (!dnsServerDomainName.matches(DNSHelper.DNS_CHECK_REGEX)) continue;

      // this lines can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
      for (DnsServer dns:domain.getDnsServer()) {
        if (dns.getName().equals(dnsServerDomainName)) continue nextdnsserver;
      }

      DnsServer dnsServer=null;
      if (dnsServerMap!=null) {
        dnsServer=dnsServerMap.get(dnsServerDomainName);
        if (dnsServer==null) dnsServerMap.put(dnsServerDomainName,dnsServer=new DnsServer(dnsServerDomainName));
      }
      else {
        dnsServer=new DnsServer(dnsServerDomainName);
      }
      domain.addDnsServer(dnsServer);
    }
    return domain;
  }
  
  public static Domain parseResInputLine(String line) {
    return parseResInputLine(line, null, null);
  }

}
