package com.gmsxo.domains;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class RegexpTest {
  private static final Logger LOG=Logger.getLogger(RegexpTest.class);
  private static final String REGEX="^[A-Z0-9]([A-Z0-9\\-\\.]*){1}( NS | IN NS ){1}[A-Z0-9]([A-Z0-9\\-\\.]*){1}$";

  /**
   * @param args
   */
  public static void main(String[] args) {
    checkTestFile(args[0],args[1],args[2]);
    //checkRegex();
  }
  
  public static void checkTestFile(String inputFile, String outputFileBad, String outputFileGood) {
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8);
         BufferedWriter writerBad =Files.newBufferedWriter(Paths.get(outputFileBad), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
         BufferedWriter writerGood=Files.newBufferedWriter(Paths.get(outputFileGood), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      while(true){
        String line=reader.readLine();
        if (line==null) break;
        if (!line.matches(REGEX)) {
          writerBad.append(line);
          writerBad.newLine();
          writerBad.flush();
        }
        else {
          writerGood.append(line);
          writerGood.newLine();
          writerGood.flush();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void checkRegex() {
    String input="SELF-DRIVE-CAR-RENTAL NS IZA.HOSTING.DIGIWEB.IE.";
    
    //String input="FO9 NS DS RRSIG#";
    
    Pattern pattern = Pattern.compile(REGEX);
    //Matcher matecher = pattern.matcher(input);
    if (input.matches(REGEX)) LOG.info("OK");
  }

}


