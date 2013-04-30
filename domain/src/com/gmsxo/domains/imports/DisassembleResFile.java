package com.gmsxo.domains.imports;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.gmsxo.domains.helpers.FileHelper;

public class DisassembleResFile {

  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    if (args.length!=0) {
      System.err.println("Usage DissasembleResFile workDir");
      System.exit(-1);
    }
    doJob("C:\\Temp\\domains\\split\\010k\\");
    doJob("C:\\Temp\\domains\\split\\050k\\");
    doJob("C:\\Temp\\domains\\split\\100k\\");
  }
  private static void doJob(String workDir) throws IOException {
    for (String fileNameIn:FileHelper.getFilesFull(workDir, ".res")) {
      try (BufferedReader reader=Files.newBufferedReader(Paths.get(fileNameIn),StandardCharsets.UTF_8);
           BufferedWriter writer=Files.newBufferedWriter(Paths.get(fileNameIn+".dom"), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
        while (true) {
          String line=reader.readLine();
          if (line==null) break;
  
          String[] splitLine=line.split(" ",-1);
          if (splitLine.length<3) continue;
          writer.append(splitLine[0]);
          writer.newLine();
        }
        writer.flush();
      }
    }

  }
}
