package com.gmsxo.domains.imports;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.Domain;

public class ExportToFileThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(ExportToFileThread.class);
  private String outputFilePathName;
  private List<Domain> domains=new LinkedList<Domain>();
  private boolean interrupted=false;
  private String finalFile;
  
  public ExportToFileThread(String workingDir, String outputFileName, String exportDir, String partFileExt, String resultFileExt) {
    outputFilePathName=workingDir+outputFileName+partFileExt;
    finalFile=exportDir+outputFileName+resultFileExt;
  }

  @Override
  public void run() {
    try (BufferedWriter output=Files.newBufferedWriter(Paths.get(outputFilePathName),StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      List<Domain> domainsToWrite=null;
      while (true) {
        domainsToWrite=getDomains();
        for (Domain domain:domainsToWrite) {
          output.append(format(domain));
          output.newLine();
        }
        if (interrupted) {
          output.flush();
          output.close();
          Files.move(Paths.get(outputFilePathName), Paths.get(finalFile), StandardCopyOption.REPLACE_EXISTING);
          break;
        }
      }
    } catch (IOException e) {
      LOG.error("IO",e);
    } 
  }
  
  public synchronized void addDomains(Domain domain) {
    domains.add(domain);
    notify();
  }
  
  private synchronized List<Domain> getDomains() {
    if (domains.size()==0 || !interrupted)
      try {
        wait();
      } catch (InterruptedException e) {
        interrupted=true;
      }
    List<Domain> retVal=domains;
    domains=new LinkedList<Domain>();
    return retVal;
  }
  
  private static final char DELIMITER=' ';
  
  private static String format(Domain domain) {
    StringBuilder sb=new StringBuilder();
    sb.append(domain.getDomainName()).append(DELIMITER);
    sb.append(domain.getIpAddress().getIpAddress());
    int dnsServersCount=domain.getDnsServer().size();
    if (dnsServersCount>0) {
      sb.append(DELIMITER);
      for (int index=0;index<dnsServersCount;index++) {
        sb.append(domain.getDnsServer().get(index).getDomainName());
        if (index<dnsServersCount-1) sb.append(DELIMITER);
      }
    }
    return sb.toString();
  }

}
