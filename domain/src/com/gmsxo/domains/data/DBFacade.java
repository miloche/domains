package com.gmsxo.domains.data;

import java.sql.SQLException;
import java.util.List;

import com.ibatis.sqlmap.client.SqlMapClient;

public final class DBFacade {
  private static SqlMapClient sqlClient = SQLMapConfig.sqlMap();

  
  @SuppressWarnings("unchecked")
  public static List<Domain> insertDomain(String domainName) throws SQLException {
    return (List<Domain>)sqlClient.insert("insertDomain", domainName);
  }
}
