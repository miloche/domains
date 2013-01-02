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
   */
  public static void main(String[] args) throws SQLException {
    Path domainFilePath = Paths.get("C:\\Temp\\domains\\com011.txt");
    long counter = 0;
    String oldDomain = "";
    try (BufferedReader reader = Files.newBufferedReader(domainFilePath, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        String domainName = line.split(" ")[0].toLowerCase()+topLevel;
        System.out.println((counter++) + " " + domainName);
        if (!oldDomain.equals(domainName)) {
          DBFacade.insertDomain(domainName);
          oldDomain = domainName;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
