package com.gmsxo.domains.data;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gmsxo.domains.db.DBFacade;
import com.gmsxo.domains.db.DBUtil;

public class ATry {
  private static final Logger LOG = Logger.getLogger(ATry.class);
  /**
   * @param args
   * @throws SQLException 
   */
  public static void main(String[] args) {
    try {
      DNSServer dns1 = new DNSServer("dn1.abc.com");
      DNSServer dns2 = new DNSServer("dn2.abc.com");
      DNSServer dns3 = new DNSServer("dn3.abc.com");
      
      IPAddress ipa1 = new IPAddress("1.2.3.4");
      IPAddress ipa2 = new IPAddress("1.2.3.5");
      
      Domain domain1 = new Domain("abc.com", ipa1).addDnsServer(dns1).addDnsServer(dns2);
      Domain domain2 = new Domain("xyz.com", ipa2).addDnsServer(dns1).addDnsServer(dns3);
      Domain domain3 = new Domain("xya.com", ipa2).addDnsServer(dns1).addDnsServer(dns3);
      
      DBFacade.saveOrUpdateDomain(domain1);
      //DBFacade.saveOrUpdateDomain(domain2);
      //DBFacade.saveOrUpdateDomain(domain3);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    DBUtil.close();
  }

}
