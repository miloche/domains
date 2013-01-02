package com.gmsxo.domains.data;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;

public final class SQLMapConfig {

  private SQLMapConfig() {}

  public static boolean isInitialized() {
    return initialized;
  }

  public static SqlMapClient sqlMap() {
    if(initialized) {
      return sqlMap;
    } else {
      System.out.println("SQLModel::sqlMap() not initialized");
      return null;
    }
  }

  private static SqlMapClient sqlMap;
  protected static final String configFile = "com/gmsxo/domains/data/SqlMapConfig.xml";
  private static boolean initialized = false;

  static {
    try {
      java.io.Reader reader = Resources.getResourceAsReader(configFile);
      sqlMap = SqlMapClientBuilder.buildSqlMapClient(reader);
      initialized = true;
    }
    catch(Exception e) {
      System.out.println("static initializer: iBatis initialization failed");
      e.printStackTrace(System.out);
    }
  }
}
