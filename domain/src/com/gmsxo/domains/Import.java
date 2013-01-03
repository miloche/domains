package com.gmsxo.domains;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import com.gmsxo.domains.data.DBFacade;

public class Import {
  public static final String topLevel = ".com";

  /**
   * @param args
   * @throws SQLException 
   * @throws IOException 
   */
  public static void main(String[] args) throws SQLException, IOException {
    //if (args.length != 1) {
    //  System.err.println("USAGE: java Import pathToImportFileDir");
    //  System.exit(-1);
    //}
    
    Path domainFilePath = Paths.get("C:\\Temp\\domains\\com011.txt");
    long counter = 0;
    String oldDomain = "";
    try (BufferedReader reader = Files.newBufferedReader(domainFilePath, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        counter++;
        if (counter < 0) continue;
        String domainName = line.split(" ")[0].toLowerCase()+topLevel;
        System.out.println(counter + " " + domainName);
        if (!oldDomain.equals(domainName)) {
          try {
            DBFacade.insertDomain(domainName);
          }
          catch (com.ibatis.common.jdbc.exception.NestedSQLException e) {
            if (e.getCause().getMessage().contains("duplicate key value")) System.out.println("duplicate");
            else throw e;
          }
          oldDomain = domainName;
        }
      }
    } 
  }

}
