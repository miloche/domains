package com.gmsxo.domains;

import org.hibernate.Query;
import org.hibernate.Session;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;

public class CheckDb {

  /**
   * @param args
   */
  public static void main(String[] args) {
    Session ses=DBUtil.openSession();
    try {
      Domain dbDomain=(Domain)ses.getNamedQuery("Domain.findByName").setString("name", "pokerhotline.com").uniqueResult();
      System.out.println(dbDomain);
      
      IpAddress dbIpAddress=(IpAddress)ses.getNamedQuery("IpAddress.findById").setInteger("id", 1).uniqueResult();
      System.out.println(dbIpAddress);
      
      
      Query query = ses.createQuery("from DnsServer where name = :key");
      query.setString("key", "domain-is-for-sale.net");

      DnsServer dbDnsServer=(DnsServer)query.uniqueResult();
      System.out.println(dbDnsServer);
    }
    finally {
      DBUtil.close();
    }

  }

}
