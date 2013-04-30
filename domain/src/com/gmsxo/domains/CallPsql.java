package com.gmsxo.domains;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;

import org.apache.log4j.Logger;

public class CallPsql {
  private static Logger LOG=Logger.getLogger(CallPsql.class);
  /**
   * @param args
   */
  public static void main(String[] args) {
    LOG.info("Calling pgloader");
    String line=null;
    try {
      int retVal=0;
      String[] cmd = {"pgloader","-c", "/home/miloxe/domains/pgloader/pgloader.conf"};
      Process p = new ProcessBuilder(cmd).redirectError(Redirect.to(new File("/dev/null"))).redirectOutput(Redirect.to(new File("/dev/null"))).start();
      try {
        retVal=p.waitFor();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      LOG.info("Pgloader returned:"+retVal);
      /*Process p = Runtime.getRuntime().exec("pgloader -c /home/miloxe/domains/pgloader/pgloader.conf");
      int retVal=0;

      BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      while ((line = in.readLine()) != null) {
        LOG.info(line);
      }
      in.close();
      in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      while ((line = in.readLine()) != null) {
        LOG.info(line);
      }
      in.close();
      
      try { retVal=p.waitFor(); } catch (InterruptedException e) { LOG.error("Interrupted", e); }*/
      

    } catch (IOException e) { 
      LOG.error("IO",e);
      System.exit(-1);
    }  }

}
