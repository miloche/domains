package com.gmsxo.domains.config;

import java.io.File;

public final class AppConfig {

  private AppConfig() {}
  
  public static final String CFG_EXPORT_SUB_DIR="export"+File.separator;
  public static final String CFG_BACKUP_SUB_DIR="backup"+File.separator;
  public static final String CFG_EXT_RES=".res";
  public static final int    CFG_MAX_ERRORS=100;
  public static final int    CFG_WAIT_FOR_NEXT_FILE_DELAY=60000;
  public static final int    CFG_INSERT_DOMAIN_BATCH_SIZE=30;
  public static final int    CFG_INSERT_IP_THREAD_COUNT=1;
  public static final int    CFG_INSERT_DNS_THREAD_COUNT=1;
  public static final int    CFG_INSERT_DOMAIN_THREAD_COUNT=2;

}
