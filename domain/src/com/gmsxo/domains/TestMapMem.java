package com.gmsxo.domains;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gmsxo.domains.helpers.DNSHelper;

public class TestMapMem {
  private static Logger LOG=Logger.getLogger(TestMapMem.class);
  public static void main(String[] args) throws IOException {
    new TestMapMem("C:\\Temp\\domains\\res\\","full-zone.com.20130113.099.cut.res").doJob();
  }
  
  public TestMapMem(final String workingDir, final String fileName) {
    this.workingDir=workingDir;
    this.fileName=fileName;
  }
  
  private String workingDir;
  private String fileName;
  private Map<String, Long> dnsMap=new TreeMap<>();
  
  public void doJob() throws IOException {
    LOG.info("doJob:"+workingDir+fileName);
    
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+fileName),StandardCharsets.UTF_8)) {
      while (true) {
        String line=reader.readLine();
        if (line==null) break;
        
        String[] splitLine=line.split(" ",-1);
        if (splitLine.length<3) continue;
        
        Set<String> dnsInOneLine=new TreeSet<String>();
        
        nextdnsserver: for (int i=2;i<splitLine.length;i++) {
          String dnsServerDomainName=splitLine[i];
          
          // this line can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
          if (!dnsServerDomainName.matches(DNSHelper.DNS_CHECK_REGEX)) continue nextdnsserver;
          
          // this lines can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
          if (dnsInOneLine.contains(dnsServerDomainName)) continue nextdnsserver;
          dnsInOneLine.add(dnsServerDomainName);

          Long dnsServerId=dnsMap.get(dnsServerDomainName);
          if (dnsServerId==null) {
            //dnsMap.put(dnsServerDomainName, (long)(java.lang.Math.random()*1000000d));
            dnsMap.put(dnsServerDomainName, null);
          }
        }
      } 
    }
    
    StringBuilder sb=new StringBuilder("select dns.id, dns.domain_name from dns_server dns where dns.domain_name in (");
    
    for (Entry<String, Long> dns:dnsMap.entrySet()) {
      sb.append("'").append(dns.getKey()).append("',");
    }
    sb.replace(sb.length()-1, sb.length(), ")");
    LOG.info(sb.toString());
    
    //Session session = DBUtil.openSession();

  }
}