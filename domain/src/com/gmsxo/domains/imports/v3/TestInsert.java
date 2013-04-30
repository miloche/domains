package com.gmsxo.domains.imports.v3;

import static com.gmsxo.domains.config.AppConfig.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.DNSHelper;

public class TestInsert {
  private static final Logger LOG=Logger.getLogger(TestInsert.class);

  public static void main(String[] args) {
    for (String s:args) System.out.println(s);
    if (args.length!=2) {
      System.err.println("Usage TestInsert workingDir fileName");
      System.exit(-1);
    }
    String workingDir=args[0];
    String fileName=args[1];
    new TestInsert(workingDir, fileName).doJob();
  }
  
  private String workingDir;
  private String fileName;
  
  public TestInsert(String workingDir, String fileName) {
    this.workingDir=workingDir;
    this.fileName=fileName;
  }

  public void doJob() {
    LOG.debug("TestInsert started");
    int counter=0;
    Session ses=DBUtil.openSession();
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+CFG_BACKUP_SUB_DIR+fileName),StandardCharsets.UTF_8)) {
        nextdomain: while (true) {
          counter++;
          String line=reader.readLine();
          if (line==null) break;

          String[] splitLine=line.split(" ",-1);
          if (splitLine.length<3) continue;
          
          Domain domain = new Domain(splitLine[0]);
          String ip=splitLine[1];
          
          domain.setIpAddress(new IpAddress(ip));
          
          nextdnsserver: for (int i=2;i<splitLine.length;i++) {
            String dnsServerDomainName=splitLine[i];
            
            // this line can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
            if (!dnsServerDomainName.matches(DNSHelper.DNS_CHECK_REGEX)) continue;
            
            // this lines can be removed later, it was added because of a bug in IPResolver which caused duplicate dns in one output line. it's fixed now
            for (DnsServer dns:domain.getDnsServer()) {
              if (dns.getName().equals(dnsServerDomainName)) continue nextdnsserver;
            }

            domain.addDnsServer(new DnsServer(dnsServerDomainName));
          }
          
          Domain d=(Domain)ses.getNamedQuery("Domain.findByName").setString("name", domain.getName()).uniqueResult();

          if (d==null) {
            LOG.error("Not found: "+domain);
            continue;
          }

          if (d.getIpAddress()==null) {
            LOG.error("Ip Address is null: "+domain+" "+d);
            continue;
          }

          if (!domain.getIpAddress().getAddress().substring(0, Math.min(domain.getIpAddress().getAddress().length(), 15)).equals(d.getIpAddress().getAddress())) {
            LOG.error("Ip Address doesn't match:"+domain+" "+d+" "+domain.getIpAddress()+" "+d.getIpAddress());
            continue;
          }

          if (d.getDnsServer()==null) {
            LOG.error("No dns server: "+domain+" "+d+" "+domain.getDnsServer()+" "+d.getDnsServer());
            continue;
          }

          for (DnsServer dnsFile:domain.getDnsServer()) {
            if (!d.getDnsServer().contains(dnsFile)) {
              LOG.error("Dns set doesn't match: "+domain+" "+d+" "+domain.getDnsServer()+" "+d.getDnsServer());
              continue nextdomain;
            }
          }

          for (DnsServer dnsFile:d.getDnsServer()) {
            if (!domain.getDnsServer().contains(dnsFile)) {
              LOG.error("Dns set doesn't match: "+domain+" "+d+" "+domain.getDnsServer()+" "+d.getDnsServer());
              continue nextdomain;
            }
          }
          if (counter%5000==0) {
            LOG.debug(counter+" 6");
            LOG.debug(domain);
            LOG.debug(d);
            LOG.debug(domain.getIpAddress());
            LOG.debug(d.getIpAddress());
            LOG.debug(domain.getDnsServer());
            LOG.debug(d.getDnsServer());
          }
        }
        LOG.debug("The rest of records");
      } catch (IOException e) { LOG.error("IO "+workingDir+CFG_BACKUP_SUB_DIR+fileName,e);}
        catch (Exception e) { LOG.error("Exception",e);}
      finally {
        if (ses!=null)ses.close();
      }
      DBUtil.close();
      LOG.debug("TestInsert finished");
  }
}
