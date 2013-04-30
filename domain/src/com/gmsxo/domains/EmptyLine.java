package com.gmsxo.domains;

import static com.gmsxo.domains.config.AppConfig.CFG_EXPORT_SUB_DIR;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.Logger;

import com.gmsxo.domains.helpers.FileHelper;

public class EmptyLine {
  static Logger LOG=Logger.getLogger(EmptyLine.class);

  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    if (args.length!=2) {
      System.err.println("The program seeks empty lines in an input file with a specified extension. Usage EmptyLine workingDir extension");
      System.exit(-1);
    }
    String workingDir=args[0];
    String ext=args[1];
    LOG.info("WorkingDir:"+workingDir);
    LOG.info("Extension:"+ext);
    long counter=0;

    List<String> fileNames=FileHelper.getFiles(workingDir, ext);
    LOG.info("Files:"+fileNames.size());
    String prevLine="";

    for (String fileName:fileNames) {
      LOG.info(fileName);
      try (BufferedReader reader=Files.newBufferedReader(Paths.get(workingDir+fileName),StandardCharsets.UTF_8)) {
        while (true) {
          counter++;
  
          String line=reader.readLine();
          if (line==null) break;
          if (line.length()==0) LOG.debug(counter+" "+prevLine+" "+fileName);
          prevLine=line;
        }
      } catch (IOException e) { LOG.error("IO",e); }
      if (counter==2)break;
    }
  }

}
