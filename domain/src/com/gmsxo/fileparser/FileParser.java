package com.gmsxo.fileparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
 
public class FileParser {
 
        public static void main(String[] args) {
            
final int MAXWORD=10;
    
    BufferedWriter[] writers = new BufferedWriter[MAXWORD+1];
    String outputFileName="C:\\temp\\output";

    
    try {
        for (int i=0;i<writers.length;i++) {
        
          writers[i]=Files.newBufferedWriter(
             Paths.get(outputFileName+i),StandardCharsets.UTF_8,
             StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        }
    }
           catch (IOException e) {
               
            for (BufferedWriter writer:writers) {
                        if (writer!=null) try {
        writer.close();
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
                     }
                     return;
            }
 
                BufferedReader br = null;
 
                try {
 
                        String sCurrentLine;
 
                        br = new BufferedReader(new FileReader("D:\\productionlab\\2names.txt"));
 
                        while ((sCurrentLine = br.readLine()) != null) {
                          
                          String split[] = sCurrentLine.split(" ",-1);
                          
                          
                          if (split.length>MAXWORD) {
                           writers[MAXWORD].write(sCurrentLine);
                           writers[MAXWORD].newLine();
                          }
                          else {
                              writers[split.length-1].write(sCurrentLine);  
                              writers[split.length-1].newLine();
                           }
                            
                                System.out.println(sCurrentLine);
                        }
 
                } catch (IOException e) {
                        e.printStackTrace();
                } finally {
                    for (BufferedWriter writer:writers) {
                        if (writer!=null) try {
                    writer.close();
                  } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                     }
                        try {
                                if (br != null)br.close();
                        } catch (IOException ex) {
                                ex.printStackTrace();
                        }
                }
 
        }
}