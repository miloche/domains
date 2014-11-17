package com.gmsxo.domains.config;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.AppClass;
import com.gmsxo.domains.db.DBUtil;

public class ConfigLoader {
  private static final Logger LOG=Logger.getLogger(ConfigLoader.class);
  
  public static ConfigLoader loadConfig(String className) {return new ConfigLoader().init(className);}

  private AppClass appClass;
  private ConfigLoader init(String className) {
    Session configSession=DBUtil.openSession();
    try {appClass=(AppClass)configSession.createQuery("from AppClass where className=:className").setParameter("className", className).uniqueResult();}
    catch (Exception e) {LOG.error("exception",e);}
    finally {if (configSession.isOpen()) configSession.close();}
    return this;
  }
  public String getString(String key) {return appClass.getValue(key);}
  public Integer getInteger(String key) {return Integer.parseInt(appClass.getValue(key));}
  public Long getLong(String key) {return Long.parseLong(appClass.getValue(key));}
  public Boolean getBoolean(String key) {return Boolean.parseBoolean(appClass.getValue(key));}
}
