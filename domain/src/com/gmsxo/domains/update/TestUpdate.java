package com.gmsxo.domains.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.ImportHelper;

public class TestUpdate {
  private static final Logger LOG = Logger.getLogger(TestUpdate.class);

  public static void main(String[] args) {
    for (String s : args)
      System.out.println(s);
    if (args.length != 1) {
      System.err.println("Usage TestInsert fullFileName");
      System.exit(-1);
    }
    String fileName = args[0];
    new TestUpdate(fileName).doJob();
  }

  private String fileName;

  public TestUpdate(String fileName) {
    this.fileName = fileName;
  }

  public void doJob() {
    LOG.debug("TestUpdate started:"+fileName);
    int counter = 0;
    Session ses = DBUtil.openSession();
    int errCounter=0;

    try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8)) {
      nextdomain : while (true) {
        String line = reader.readLine();
        if (line == null)
          break;
        Domain domain = ImportHelper.parseResInputLine(line);
        if (domain == null) {
          LOG.warn("Domain is null for input line: " + line);
          continue nextdomain;
        }

        counter++;
        try {
          Query domainQuery = ses.createSQLQuery("SELECT d.id,d.name,0 as ip_address_id FROM domain d WHERE d.name='" + domain.getName() + "'").addEntity(Domain.class);
          
          Domain dbDomain = (Domain) domainQuery.uniqueResult();
          if (dbDomain == null) {
            LOG.warn("Missing:" + domain);
            continue nextdomain;
          }
  
          Query ipIdQuery = ses.createSQLQuery("SELECT ip_address_id FROM domain_ip_address_lnk WHERE domain_id=" + dbDomain.getId());
          Integer ipAddressId = (Integer)ipIdQuery.uniqueResult();
  
          if (ipAddressId == null) {
            LOG.warn("Missing iplink:" + domain);
            continue nextdomain;
          }
  
          Query ipAddressQuery = ses.createSQLQuery("SELECT id,address FROM ip_address WHERE id=" + ipAddressId).addEntity(IpAddress.class);
          IpAddress dbIpAddress = (IpAddress) ipAddressQuery.uniqueResult();
  
          if (dbIpAddress == null) {
            LOG.warn("Missing linked ip address:" + domain + " " + ipAddressId);
          } else if (!dbIpAddress.getAddress().equals(domain.getIpAddress().getAddress())) {
            String ipAddressStr=domain.getIpAddress().getAddress();
            if (ipAddressStr.charAt(0)>='0'&&ipAddressStr.charAt(0)<='9')
              LOG.warn("IP address doesn't match:" + domain + " " + domain.getIpAddress() + " " + dbIpAddress);
          }
  
          Query dnsIdListQuery = ses.createSQLQuery("SELECT dns_server_id FROM domain_dns_server_lnk WHERE domain_id=" + dbDomain.getId());
          @SuppressWarnings("unchecked")
          List<Integer> dnsIdList = (List<Integer>) dnsIdListQuery.list();
  
          if (dnsIdList == null) {
            LOG.warn("No DNS server list:" + domain);
            continue nextdomain;
          }
  
          List<DnsServer> dbDnsList = new ArrayList<>();
  
          for (Integer dnsId : dnsIdList) {
            Query dnsQuery = ses.createSQLQuery("SELECT id,name FROM dns_server WHERE id=" + dnsId).addEntity(DnsServer.class);
            DnsServer dbDns = (DnsServer) dnsQuery.uniqueResult();
            dbDnsList.add(dbDns);
          }
  
          if (dbDnsList.size() != dbDomain.getDnsServer().size()) {
            LOG.warn("DNS server list size doesn't match:" + domain + " " + domain.getDnsServer() + " " + dbDnsList);
            continue nextdomain;
          }
  
          for (DnsServer dns : domain.getDnsServer()) {
            if (!dbDnsList.contains(dns)) {
              LOG.warn("DNS server list doesn't match left:" + domain + " " + domain.getDnsServer() + " " + dbDnsList);
              break;
            }
          }
  
          for (DnsServer dns : dbDnsList) {
            if (!domain.getDnsServer().contains(dns)) {
              LOG.warn("DNS server list doesn't match right:" + domain + " " + domain.getDnsServer() + " " + dbDnsList);
              break;
            }
          }
          if (counter%10000==0) LOG.info("Line:"+counter);
          LOG.debug(domain+" "+domain.getIpAddress()+" "+domain.getDnsServer());
          LOG.debug(dbDomain+" "+dbIpAddress+" "+dbDnsList);
        }
        catch (Exception e) {
          LOG.error("Excpetion:",e);
          LOG.error("Cause:"+e.getCause());
          for (StackTraceElement el:e.getStackTrace()) {
            LOG.error("El.:"+el+" "+el.getClassName()+" "+el.getMethodName()+" "+el.getFileName());
          }
          if (errCounter++>10) {
            LOG.error("Too many exceptions, exitting");
            System.exit(1);
          }
          if (!ses.isOpen()) ses=DBUtil.openSession();
        }
        
      }
      LOG.debug("TestUpdate finished:"+fileName);
    } catch (IOException e) { LOG.error("IO " + fileName, e); }
      catch (Exception e) {
        LOG.error("Exception", e);
        LOG.error("Cause:"+e.getCause());
        for (StackTraceElement el:e.getStackTrace()) {
          LOG.error("El.:"+el+" "+el.getClassName()+" "+el.getMethodName()+" "+el.getFileName());
        }
    } finally {if (ses != null) if (ses.isOpen()) ses.close();}
    DBUtil.close();
    LOG.debug("TestInsert finished");
  }
}
