package com.gmsxo.domains.resolve.v1;

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

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;

public class ExportToFileThread implements Runnable {
  private static final Logger LOG=Logger.getLogger(ExportToFileThread.class);
  private static final char   DELIMITER=' ';
  
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
    try (BufferedWriter output=Files.newBufferedWriter(Paths.get(outputFilePathName),StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
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
          LOG.debug("moved");
          break;
        }
      }
    } catch (IOException e) { LOG.error("IO",e); } 
  }
  
  public synchronized void addDomains(Domain domain) {
    domains.add(domain);
    notify();
  }
  
  private synchronized List<Domain> getDomains() {
    if (domains.size()==0 || !interrupted)  try {  LOG.trace("waiting"); wait();  } catch (InterruptedException e) {  LOG.trace("interrupted"); interrupted=true;   }
    List<Domain> retVal=domains;
    domains=new LinkedList<Domain>();
    return retVal;
  }
  
  private static String format(Domain domain) {
    StringBuilder sb=new StringBuilder();
    sb.append(domain.getName()).append(DELIMITER);
    sb.append(domain.getIpAddress().getAddress());
    int dnsServersCount=domain.getDnsServer().size();
    if (dnsServersCount>0) {
      sb.append(DELIMITER);
      int index=0;
      for (DnsServer dns:domain.getDnsServer()) {
        sb.append(dns.getName());
        if (index++<dnsServersCount-1) sb.append(DELIMITER);
      }
    }
    return sb.toString();
  }
}
