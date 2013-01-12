package com.gmsxo.domains.data;

import java.sql.SQLException;
import java.util.List;

import com.ibatis.sqlmap.client.SqlMapClient;

public final class DBFacade {
  private static SqlMapClient sqlClient = SQLMapConfig.sqlMap();
  
  /* Domains */

  public static Integer insertDomain(String domainName) throws SQLException {
    return (Integer)sqlClient.insert("insertDomain", domainName);
  }
  
  @SuppressWarnings("unchecked")
  public static List<Domain> getDomais(int dnsLookUpJobId) throws SQLException {
    return (List<Domain>)sqlClient.queryForList("getDomains", dnsLookUpJobId);
  }
  
  public static Integer updateDomainIpAddressId(Domain domain) throws SQLException {
    return (Integer)sqlClient.update("updateDomainIpAddressId", domain);
  }
  
  /* IP Address */
  
  public static Integer insertIPAddress(IPAddress ipAddress) throws SQLException {
    return (Integer)sqlClient.insert("insertIPAddress", ipAddress);
  }
  
  public static IPAddress getIPAddress(int id) throws SQLException {
    return (IPAddress)sqlClient.queryForObject("getIPAddress", id);
  }
  
  public static Integer getIPAddressId(String ipAddress) throws SQLException {
    return (Integer)sqlClient.queryForObject("getIPAddressId", ipAddress);
  }
  
  /* DNS */
  
  public static Integer insertDnsLookupJob(DNSLookUpJob dnsLookUpJob) throws SQLException {
    return (Integer)sqlClient.insert("insertDNSLookUpJOb", dnsLookUpJob);
  }
  
  public static DNSServer getNextDNSServer(int lastDNSServerId) throws SQLException {
    return (DNSServer)sqlClient.queryForObject("getNextDNSServer", lastDNSServerId);
  }
}
