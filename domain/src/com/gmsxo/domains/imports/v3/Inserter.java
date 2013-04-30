package com.gmsxo.domains.imports.v3;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.Insertable;
import com.gmsxo.domains.db.DBUtil;

import static com.gmsxo.domains.config.AppConfig.*;

public class Inserter implements Runnable {
  private static final Logger LOG=Logger.getLogger(Inserter.class);
  private TreeMap<String, Insertable> records;
  
  @SuppressWarnings("unchecked")
  public Inserter(TreeMap<String, ? extends Insertable> records) {this.records=(TreeMap<String, Insertable>)records;}
  
  @Override
  public void run() {
    try {
      LOG.debug("Inserter started");
      ExecutorService insertIpPool=Executors.newFixedThreadPool(CFG_INSERT_IP_THREAD_COUNT);
      String from=records.firstKey();
      int counter=0;
      for (String rec:records.keySet()) {
        if (counter++==CFG_INSERT_IP_THREAD_RECORDS) {
          insertIpPool.submit(new InserterThread(records.subMap(from, rec)));
          counter=1;
          from=rec;
        }
      }
      insertIpPool.submit(new InserterThread(records.tailMap(from)));
      insertIpPool.shutdown();
      try {
        insertIpPool.awaitTermination(1, TimeUnit.HOURS);
      } catch (InterruptedException e) { LOG.error("Interrupted", e);}
      LOG.debug("Inserter finished");
    } catch (Exception e) {LOG.error("Exception",e);}
  }
}

class InserterThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(InserterThread.class);
  
  private SortedMap<String, Insertable> records;
  
  public InserterThread(SortedMap<String, Insertable> records) { this.records=records; }
  
  @Override
  public void run() {
    LOG.debug("Inserter thread started: records "+records.size());
    Session ses = DBUtil.openSession();
    try {
      if (records==null || records.size()==0) return;
      StringBuilder insert=records.get(records.firstKey()).getInsertRoot();
      for (Insertable rec:records.values()) {
        Number ipAddrId=(((Number)ses.createSQLQuery(rec.getQuery()).uniqueResult()));
        if (ipAddrId!=null) {
          rec.setInsertedId(ipAddrId.intValue());
          continue;
        }
        insert.append("('").append(rec.getKeyValue()).append("'),");
      }
      if (insert.length()>45) { // there are some ip to insert
        insert.setLength(insert.length()-1); // remove the last ','
        try {
          ses.beginTransaction();
          LOG.debug(insert.toString());
          int res = ses.createSQLQuery(insert.toString()).executeUpdate();
          ses.getTransaction().commit();
          LOG.debug("Commited: "+res);
        }
        catch (Exception e) {
          LOG.error("Transaction",e);
          ses.getTransaction().rollback();
        }
      }
      for (Insertable rec:records.values()) // set ids of just inserted ips
        if (rec.getId()==null) rec.setInsertedId((((Number)ses.createSQLQuery(rec.getQuery()).uniqueResult())).intValue());
    }
    catch (Exception e) { LOG.error("Exception",e); }
    finally {ses.close();}
    LOG.debug("Inserter thread finished");
  }
}
