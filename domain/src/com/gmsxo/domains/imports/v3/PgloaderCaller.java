package com.gmsxo.domains.imports.v3;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.apache.log4j.Logger;

public class PgloaderCaller implements Runnable {
  private static final Logger LOG=Logger.getLogger(PgloaderCaller.class);
  private String configFile;
  
  public PgloaderCaller(String configFile) { this.configFile=configFile; }
  
  @Override
  public void run() {
    LOG.info("Calling pgloader...");
    try {
      int retVal=0;
      String[] cmd = {"pgloader","-c", configFile};
      Process p = new ProcessBuilder(cmd).redirectError(Redirect.to(new File("/dev/null"))).redirectOutput(Redirect.to(new File("/dev/null"))).start();
      try { retVal=p.waitFor(); } catch (InterruptedException e) { LOG.error("Interrupted",e); }
      LOG.info("Pgloader returned:"+retVal);
      if (retVal!=0 && retVal!=1) {
        LOG.error("Pgloader:"+retVal);
        System.exit(-1);
      }
    } catch (IOException e) { 
      LOG.error("IO",e);
      System.exit(-1);
    } catch (Exception e) { 
      LOG.error("Exception",e);
      System.exit(-1);
    }
  }
}
