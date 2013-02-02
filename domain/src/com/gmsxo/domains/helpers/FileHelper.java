package com.gmsxo.domains.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gmsxo.domains.dns.DNSLookup;

public final class FileHelper {
  @SuppressWarnings("unused") private static final Logger LOG = Logger.getLogger(FileHelper.class); 
  private FileHelper() {}
  
  public static List<String> getFiles(String dirName, String extension) throws IOException {
    List<String> paths=new LinkedList<>();
    try (DirectoryStream<Path> ds=Files.newDirectoryStream(FileSystems.getDefault().getPath(dirName))) {
      for (Path path : ds) {
        File file = path.toFile();
        if(file.isFile()&&(extension==null||extension.length()==0||file.getName().endsWith(extension))) paths.add(file.getName());
      }
    } 
    return paths;
  }
  
  public static String getNextFile(String dir, String ext) throws IOException {
    List<String> files = FileHelper.getFiles(dir, ext);
    Collections.sort(files);
    if (files==null||files.size()<1) return null;
    return files.get(0);
  }
  
  public static void cutFileIntoParts(String sourceDirName, String sourceFileName, String targetDirName, int parts, String appendix) throws IOException {
    String importFilePath=sourceDirName+sourceFileName;
    long   linesCount=count(importFilePath);
    long   linesInOneFile=linesCount/parts;
    long   lineCounter=linesInOneFile+1l;
    int    fileNumber=0;
    BufferedWriter writer=null;
    
    try (BufferedReader reader=Files.newBufferedReader(Paths.get(importFilePath), StandardCharsets.UTF_8)) {
      while (true) {
        String line=reader.readLine();
        if (line==null) break;
        if (lineCounter++>=linesInOneFile){
          lineCounter=0;
          if (writer!=null) { writer.flush(); writer.close(); }
          writer=Files.newBufferedWriter(Paths.get(targetDirName+sourceFileName+"."+String.format("%03d", fileNumber++)+appendix),
                                          StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
        writer.append(line);  writer.newLine();
      }
    }
    finally {
      if (writer!=null) {
        writer.flush();
        writer.close();
      }
    }
  }
  
  public static void cutDomainFile(String sourceDirName, String sourceFileName, String targetDirName, int domainsCountInOneFile, String topLevel, String extension) throws IOException {
    String importFilePath=sourceDirName+sourceFileName;
    int    appendix=0;
    String lastDomain="";
    //long   lineCounter=0;
    long   domainCounter=0l;

    try (BufferedReader reader=Files.newBufferedReader(Paths.get(importFilePath),StandardCharsets.UTF_8)) {
      //long   sourceLinesCount=FileHelper.count(importFilePath);

      BufferedWriter writer = null;
      while (true) {
        String line = reader.readLine();
        if (line==null) break;
        if (!line.matches(DNSHelper.DOMAIN_REGEXP)) continue;
        //lineCounter++;
        String domainName=DNSLookup.formatDomain(line.split(" ",-1)[0],topLevel); // wouldn't work for other import files
        if (!lastDomain.equals(domainName)) {
          domainCounter++;
          lastDomain=domainName;
          if (domainCounter>=domainsCountInOneFile||writer==null) {// new file
            domainCounter=0;
            if (writer!=null) {
              writer.flush();
              writer.close();
            }
            String targetFileName=targetDirName+sourceFileName+"."+String.format("%03d", appendix++)+extension;
            Path newFile = Paths.get(targetFileName);
            Files.deleteIfExists(newFile);
            newFile = Files.createFile(newFile);
            writer = Files.newBufferedWriter(newFile, StandardCharsets.UTF_8);
            //LOG.info(lineCounter+"/"+sourceLinesCount+" "+targetFileName);
          }
        }
        writer.append(line);
        writer.newLine();
      }
      if (writer!=null) {
        writer.flush();
        writer.close();
      }
      reader.close();
    }
  }
  public static int count(String filename) throws IOException {
    InputStream is = new BufferedInputStream(new FileInputStream(filename));
    try {
      byte[] c = new byte[1024];
      int count = 0;
      int readChars = 0;
      boolean empty = true;
      while ((readChars = is.read(c)) != -1) {
        empty = false;
        for (int i = 0; i < readChars; ++i) {
          if (c[i] == '\n')
            ++count;
        }
      }
      return (count == 0 && !empty) ? 1 : count;
    } finally {
      is.close();
    }
  }

  public static String tail(File file) {
    RandomAccessFile fileHandler = null;
    try {
      fileHandler = new RandomAccessFile(file, "r");
      long fileLength = file.length() - 1;
      StringBuilder sb = new StringBuilder();

      for (long filePointer = fileLength; filePointer != -1; filePointer--) {
        fileHandler.seek(filePointer);
        int readByte = fileHandler.readByte();

        if (readByte == 0xA) {
          if (filePointer == fileLength) {
            continue;
          } else {
            break;
          }
        } else if (readByte == 0xD) {
          if (filePointer == fileLength - 1) {
            continue;
          } else {
            break;
          }
        }

        sb.append((char) readByte);
      }

      String lastLine = sb.reverse().toString();
      return lastLine;
    } catch (java.io.FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (java.io.IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (fileHandler != null)
        try {
          fileHandler.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
    }
  }
}
