package com.gmsxo.domains.update;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;

public class UpdateIpAddressTable {
  private static Logger LOG=Logger.getLogger(UpdateIpAddressTable.class);
  /**
   * @param args
   */
  public static void main(String[] args) {
    new UpdateIpAddressTable().doUpdate();
  }
  public void doUpdate() {
    try {
      Session session;
      
      session = DBUtil.openSession();
      Integer count = (Integer)session.createSQLQuery("select max(id) from ip_address").uniqueResult();
      LOG.debug(count);
      session.close();
      
      final int PAGE_SIZE=2000;
      int processedPages=0;
      int total=0;
      
      while (true) {
        session = DBUtil.openSession();
        session.beginTransaction();
        @SuppressWarnings("unchecked")
        List<IpAddress> ipList=session.createQuery("from IpAddress ip where ip.id>=:start and ip.id<:end").setParameter("start",processedPages*PAGE_SIZE).setParameter("end",(processedPages+1)*PAGE_SIZE).list();
        for (IpAddress ip:ipList) {
          ip.setSortAddress(getSortedAddress(ip.getAddress()));
          LOG.debug(ip);
          if (ip.getSortAddress()!=null) session.save(ip);
        }
        total+=ipList.size();
        LOG.info(ipList.size() + " "+total);
        session.getTransaction().commit();
        session.flush();
        session.close();
        if (++processedPages*PAGE_SIZE>count) break;
      }
      LOG.info(total);
    }
    finally {
      DBUtil.close();
      LOG.info("done");
    }
  }
  
  
  
  public static String getSortedAddress(String ip) {
    String[] split=ip.split("\\.",-1);
    if (split.length!=4) return null;
    StringBuilder sb=new StringBuilder(12);
    for (String sp:split) sb.append(String.format("%3s", sp).replace(' ','0'));
    return sb.toString();
  }
}
