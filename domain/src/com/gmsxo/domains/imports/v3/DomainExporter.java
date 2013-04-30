package com.gmsxo.domains.imports.v3;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.Domain;

import static com.gmsxo.domains.config.AppConfig.CFG_PGLOADER_SUB_DIR;
import static com.gmsxo.domains.config.AppConfig.CFG_WORKING_SUB_DIR;
import static com.gmsxo.domains.helpers.ImportHelper.*;

public class DomainExporter implements Runnable {
  private static final Logger LOG=Logger.getLogger(DomainExporter.class);
  
  private Set<Domain> domains;
  private String workingDir;
  public DomainExporter(String workingDir, Set<Domain> domains) { this.workingDir=workingDir; this.domains=domains; }

  @Override
  public void run() {
    LOG.debug("DomainExporter started");
    BufferedWriter[] output=new BufferedWriter[pref.length];
    try {
      for (int i=0;i<pref.length;i++) {
        output[i]=Files.newBufferedWriter(Paths.get(workingDir+CFG_WORKING_SUB_DIR+CFG_PGLOADER_SUB_DIR+pref[i]+".dom"),
            StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
      }
      for (Domain domain:domains) {
        int index=getCharIndex(domain.getName());
        output[index].write(domain.getName());
        output[index].newLine();
      }
    } catch (IOException e) { LOG.error("IO",e); }
      catch (Exception e) { LOG.error("Exception",e); }
    finally{
      for (int i=0;i<pref.length;i++) {
        if (output[i]!=null) {
          try {
            output[i].flush();
            output[i].close();
          } catch (IOException e) { LOG.error("IO"); }
        }
      }
    }
    LOG.debug("DomainExporter finished");
  }
}
