package com.gmsxo.domains.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class ImportLogAnalyzer {
  private static final Logger LOG=Logger.getLogger(ImportLogAnalyzer.class);
  private static final long day=86400000l;

  public static long getDuration(Date start, Date end) {
    if (start.getTime()<end.getTime()) return (end.getTime()-start.getTime())/1000l;
    return (end.getTime()+day-start.getTime())/1000l;
  }
  
  public static void main(String[] args) throws ParseException {
    try (BufferedReader input=Files.newBufferedReader(Paths.get("C:\\Temp\\domains\\insertdomains.log"), StandardCharsets.UTF_8)) {
      int counter=0;
      Date start=null;
      Date end=null;
      StringBuilder sb=new StringBuilder();

      while (true) {
        String line=input.readLine();
        if (line==null) break;
        //LOG.debug(String.format("%03d", counter)+" "+sb.toString()+" "+line);
        if (counter==1) {
          sb.append(line.substring(55, line.length()));
          start=new SimpleDateFormat("HH:mm:ss,SSS").parse(line.substring(0,13));
        }
        else if (counter==2) {
          end  =new SimpleDateFormat("HH:mm:ss,SSS").parse(line.substring(0,13));
          sb.append(" SPLIT: ").append(String.format("%04d",getDuration(start,end))).append("s ");
          start=end;
        }
        else if (counter==3) {
          end  =new SimpleDateFormat("HH:mm:ss,SSS").parse(line.substring(0,13));
          sb.append(" PG1: ").append(String.format("%04d",getDuration(start,end))).append("s ");
          start=end;
        }
        else if (counter==5) {
          end  =new SimpleDateFormat("HH:mm:ss,SSS").parse(line.substring(0,13));
          sb.append(" PG2: ").append(String.format("%04d",getDuration(start,end))).append("s ");
          start=end;
        }
        else if (counter==7) {
          end  =new SimpleDateFormat("HH:mm:ss,SSS").parse(line.substring(0,13));
          sb.append(" LNK FILE: ").append(String.format("%04d",getDuration(start,end))).append("s ");
          start=end;
        }
        else if (counter==9) {
          end  =new SimpleDateFormat("HH:mm:ss,SSS").parse(line.substring(0,13));
          sb.append(" PG3: ").append(String.format("%04d",getDuration(start,end))).append("s ");
          start=end;
        }
        else if (counter==12) {
          end  =new SimpleDateFormat("HH:mm:ss,SSS").parse(line.substring(0,13));
          sb.append(" PG4: ").append(String.format("%04d",getDuration(start,end))).append("s ");
          start=end;
        }
        
        counter++;
        if (counter==15) {
          LOG.info(sb.toString());
          counter=0;
          sb=new StringBuilder();
        }
      }
    } catch (IOException e) { LOG.error("IO",e); }
  }
}
