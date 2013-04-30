package com.gmsxo.domains.update;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;

public class TryThis {
  private static final Logger LOG=Logger.getLogger(TryThis.class);

  /**
   * @param args
   */
  public static void main(String[] args) {
    Session ses=DBUtil.openSession();
    try {
      Domain domain1=(Domain)ses.getNamedQuery("Domain.findByName").setString("name", "dooozy.com").uniqueResult();
      LOG.info(domain1);
      if (domain1!=null) {
        LOG.info(domain1.getIpAddress()==null?"":domain1.getIpAddress());
        for (DnsServer dns:domain1.getDnsServer()) LOG.info(dns);
      }
      else {
        LOG.info("null");
        return;
      }

      Domain domain2=(Domain)ses.getNamedQuery("Domain.findByName").setString("name", "doonpulse.com").uniqueResult();
      LOG.info(domain2);
      if (domain2!=null) {
        LOG.info(domain2.getIpAddress()==null?"":domain2.getIpAddress());
        for (DnsServer dns:domain2.getDnsServer()) LOG.info(dns);
      }
      else {
        LOG.info("null");
        return;
      }
      
      //Domain domain3=new Domain("dxxxxxx.com");
      //domain3.setIpAddress(domain2.getIpAddress());
      
      //domain1.setIpAddress(domain2.getIpAddress());
      //DnsServer dns=new DnsServer("dnsxxx.com");
      //domain1.getDnsServer().add(dns);
      for (DnsServer dns:domain1.getDnsServer()) {
        if (dns.getName().equals("dnsxxx.com")) {
          domain1.getDnsServer().remove(dns);
          break;
        }
      }
      LOG.info(domain1.getIpAddress()==null?"":domain1.getIpAddress());
      for (DnsServer dnsd:domain1.getDnsServer()) LOG.info(">>"+dnsd);
      
      Transaction tx=ses.beginTransaction();
      //ses.save(dns);
      ses.save(domain1);
      //ses.save(domain2);
      ses.flush();
      tx.commit();
      ses.close();

    }
    catch (Exception e) {LOG.error("Exception",e);}
    finally {DBUtil.close();}
  }
}
