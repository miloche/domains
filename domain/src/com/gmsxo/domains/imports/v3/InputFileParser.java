package com.gmsxo.domains.imports.v3;

import static com.gmsxo.domains.config.AppConfig.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.helpers.DNSHelper;
import com.gmsxo.domains.helpers.ImportHelper;

public class InputFileParser implements Runnable {
  private static final Logger LOG=Logger.getLogger(InputFileParser.class);
  
  private Set<Domain> domains;
  
  Map<String, IpAddress> ipAddressMap;
  Map<String, DnsServer> dnsServerMap;
  
  private String workingDir;
  private String inputFileName;
  
  public InputFileParser(String workingDir, String inputFileName, Set<Domain> domains, Map<String, IpAddress> ipAddressMap, Map<String, DnsServer> dnsServerMap) {
    this.workingDir=workingDir;
    this.inputFileName=inputFileName;
    this.domains=domains;
    this.ipAddressMap=ipAddressMap;
    this.dnsServerMap=dnsServerMap;
  }

  @Override
  public void run() {
    LOG.debug("InputFileParser started");
    int counter=0;
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+CFG_EXPORT_SUB_DIR+inputFileName),StandardCharsets.UTF_8)) {
        while (true) {
          counter++;
          String line=reader.readLine();
          if (line==null) break;

          Domain domain = ImportHelper.parseResInputLine(line,ipAddressMap,dnsServerMap);
          if (domain==null) continue;
          


          domains.add(domain);
          LOG.debug(String.format("%05d", counter)+" "+domain);
          //if (%50000==0) LOG.debug(counter);
        }
        LOG.debug("The rest of records");
      } catch (IOException e) { LOG.error("IO "+workingDir+CFG_EXPORT_SUB_DIR+inputFileName,e);}
        catch (Exception e) { LOG.error("Exception",e);}
      LOG.debug("InputFileParser finished");
  }
}
