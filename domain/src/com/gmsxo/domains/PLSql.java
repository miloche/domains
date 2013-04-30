package com.gmsxo.domains;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;

import com.gmsxo.domains.db.DBUtil;

public class PLSql {
  private static final Logger LOG=Logger.getLogger(PLSql.class);

  /**
   * @param args
   */
  public static void main(String[] args) {
    Session session=DBUtil.openSession();
    session.beginTransaction();
    //Query query=session.getNamedQuery("populate");
    Query query=session.createSQLQuery("select 1").addScalar("retVal", IntegerType.INSTANCE);
    
    List list=query.list();
    for (Object o:list) LOG.info(o.getClass()+" "+o.toString());
    session.getTransaction().commit();
    session.close();
    DBUtil.close();
  }

}
