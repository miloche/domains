package com.gmsxo.domains.config;

import java.io.File;

public final class AppConfig {

  private AppConfig() {}
  
  public static final String CFG_EXPORT_SUB_DIR="export"+File.separator;
  public static final String CFG_BACKUP_SUB_DIR="backup"+File.separator;
  public static final String CFG_WORKING_SUB_DIR="working"+File.separator;
  public static final String CFG_PGLOADER_SUB_DIR="pgloader"+File.separator;
  public static final String CFG_EXT_RES=".res";
  public static final String CFG_EXT_CUT=".cut";
  public static final String CFG_EXT_BAK=".bak";
  public static final String CFG_EXT_PART=".part";
  public static final int    CFG_MAX_ERRORS=100;
  public static final int    CFG_WAIT_FOR_NEXT_FILE_DELAY=10000;
  public static final int    CFG_INSERT_DOMAIN_BATCH_SIZE=200;
  public static final int    CFG_INSERT_IP_THREAD_COUNT=10;
  public static final int    CFG_INSERT_IP_THREAD_RECORDS=10000;
  public static final int    CFG_INSERT_DNS_THREAD_COUNT=5;
  public static final int    CFG_INSERT_DOMAIN_THREAD_COUNT=10;
  public static final int    CFG_INSERT_DOMAIN_THREAD_RECORDS=50000;
  
  public static final String[] CFG_TOPLEVELS={".biz",".com",".info",".net",".org",".us"};

}
