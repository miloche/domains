package com.gmsxo.domains.resolve.v1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;

public class ExportToFileThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(ExportToFileThread.class);
  private static final char   DELIMITER=' ';
  
  private List<Domain> domains=new LinkedList<Domain>();
  private String outputFilePathName;
  private String finalFilePathName;
  
  private volatile boolean finished=false;
  
  public ExportToFileThread(String workingDir, String outputFileName, String exportDir, String partFileExt, String resultFileExt) {
    outputFilePathName=workingDir+outputFileName+partFileExt;
    finalFilePathName=exportDir+outputFileName+resultFileExt;
  }
  /**
   * Waits for the domains in the list and writes them a the given file. When it's interrupted it renames the file with a given extension.
   * 
   * @see java.lang.Runnable#run() 
   */
  @Override
  public void run() {
    LOG.debug("run()");
    try (BufferedWriter output=Files.newBufferedWriter(Paths.get(outputFilePathName),StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
      List<Domain> domainsToWrite=null;
      while (true) {
        try {
          LOG.trace("run.getDomains()");
          domainsToWrite=getDomains();
        } catch (InterruptedException e) {
          LOG.trace("run.interrupted");
          output.flush();
          output.close();
          if (finished) Files.move(Paths.get(outputFilePathName), Paths.get(finalFilePathName), StandardCopyOption.REPLACE_EXISTING);
          LOG.trace("run.moved");
          break;
        }
        for (Domain domain:domainsToWrite) {
          output.append(format(domain));
          output.newLine();
        }
      }
    } catch (IOException e) { LOG.error("IO",e); }
    LOG.debug("run() end");
  }
  /**
   * Set the finished flag to move the output file into the final folder.
   */
  public void finished() {finished=true;}
  /**
   * Adds a given domain into the domain list and notifies waiting thread to return the domain list.
   * 
   * @param domain
   */
  public synchronized void addDomain(Domain domain) {
    LOG.debug("addDomain: "+domain);
    domains.add(domain);
    notify();
    LOG.debug("addDomain end: "+domain);
  }
  /**
   * Returns the domain list to be exported to the file and assigns a new empty one to the list variable.
   * 
   * @return
   * @throws InterruptedException
   */
  private synchronized List<Domain> getDomains() throws InterruptedException {
    LOG.debug("getDomains()");
    try {
      while (domains.size()==0) wait();
      List<Domain> retVal=domains;
      domains=new LinkedList<Domain>();
      return retVal;
    }
    finally {LOG.debug("getDomains() end");}
  }
  private static String format(Domain domain) {
    StringBuilder sb=new StringBuilder(domain.getName()).append(DELIMITER).append(domain.getIpAddress().getAddress());
    for (DnsServer dns:domain.getDnsServer()) sb.append(DELIMITER).append(dns.getName());
    return sb.toString();
  }
  static class ExceptionHandler implements UncaughtExceptionHandler {public void uncaughtException(Thread t, Throwable e) {LOG.error("uncaught",e);}}
}
