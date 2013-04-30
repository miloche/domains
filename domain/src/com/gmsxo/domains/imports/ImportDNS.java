package com.gmsxo.domains.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.dns.DNSLookup;
import com.gmsxo.domains.resolve.NSLookupThread;

public class ImportDNS {

  public static void main(String[] args) throws IOException, NamingException {
    //while (!DBFacade.initialized)
    //  ;
    if (args.length != 6) {
      try {
        new ImportDNS().doThreadJob("C:\\Temp\\domains\\full-zone.com.cut",".com",150,150,1500,1000);
      } catch (Exception e) {
        System.err.println("USAGE: java ImportDNS pathToImportFileDir poolSize requestCount timeout");
        LOG.error("main",e);
        System.exit(-1);
      }
    }else {
      NSLookupThread.topLevel=args[1];
      new ImportDNS().doThreadJob(args[0],args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[4]),Integer.parseInt(args[4]));
    }
    //DBUtil.close();
    LOG.info("lines: " + counter + " requests: "+requestCounter+" results: "+resultCounter);
  }
  private static final Logger LOG = Logger.getLogger(ImportDNS.class);
  private static final long startFrom=5771;
  private BufferedReader reader;
  private Domain lastDomain=new Domain();
  private String topLevel;
  static long counter=0l;
  private Domain getNextDomain() throws IOException {
    
    Domain returnDomain = null;
    while (true) {
      String nextLine = reader.readLine();
      if (counter++<startFrom) continue;
      String[] split=nextLine.split(" ",-1);
      if (split.length<2) return null;
      String domainName=DNSLookup.formatDomain(split[0],topLevel);

      // dns part
      
      String dnsDomainName=DNSLookup.formatDNS(split[2],topLevel);
      DnsServer dnsServer = new DnsServer();
      dnsServer.setName(dnsDomainName);
      
      if (domainName.equals(lastDomain.getName())) { // it is the same domain, add dns server and continue
        //lastDomain.getDnsServer().add(dnsServer);
        continue;
      } else { // it is the next domain, create new domain, update return domain and upadte the last domain with the new one
        Domain domain=new Domain();
        domain.setName(domainName);
        //domain.getDnsServer().add(dnsServer);
        
        if (lastDomain.getName()==null) { // the first line
          lastDomain=domain;
          continue;
        }
        returnDomain=lastDomain;
        lastDomain=domain;
        break;
      }
    }
    return returnDomain;
  }
  static long requestCounter=0;
  static long resultCounter=0;
  
  public void doThreadJob(final String fileName, String topLevel, final int poolSize, final int pool2Size, final int requestCount, final int timeout) {
    final int  DNS_LOOKUP_POOL_SIZE=poolSize;
    final int  INSERT_DOMAIN_POOL_SIZE=pool2Size;
    final long REQUEST_COUNT=requestCount;
    int insertDomainCounter=0;
    this.topLevel=topLevel;

    Path domainFilePath = Paths.get(fileName);
    ExecutorService insertDomainPool=Executors.newFixedThreadPool(INSERT_DOMAIN_POOL_SIZE);
    ExecutorService nsLookupPool=Executors.newFixedThreadPool(DNS_LOOKUP_POOL_SIZE);
    List<Future<NSLookupThread.Result>> nsLookupResultList=new ArrayList<>();
    List<Future<Object>> insertDomainResultList=new ArrayList<>();
    long startTime=new Date().getTime();
    try (BufferedReader reader=Files.newBufferedReader(domainFilePath,StandardCharsets.UTF_8)) {
      this.reader=reader;
      String line="";
      
      // fill the pool
      
      for (int i=0;i<(DNS_LOOKUP_POOL_SIZE<REQUEST_COUNT?DNS_LOOKUP_POOL_SIZE:REQUEST_COUNT)&&line!=null;i++) {
        nsLookupResultList.add(nsLookupPool.submit(new NSLookupThread(getNextDomain(),timeout)));
        requestCounter++;
      }
      
      // finish the input file
      //int indexInInsertDomainResultList=0;
      for (int i=0;;) {
        if (nsLookupResultList.get(i).isDone()) {
          try {
            NSLookupThread.Result result=nsLookupResultList.get(i).get();
            LOG.info(resultCounter+" / "+result.getDomain());
            /*if (insertDomainCounter++<INSERT_DOMAIN_POOL_SIZE) insertDomainResultList.add(insertDomainPool.submit(new InsertDomainThread(result.getDomain())));
            else {
              loop: while (true) {
                if (insertDomainResultList.get(indexInInsertDomainResultList).isDone()) {
                try {
                    insertDomainResultList.get(indexInInsertDomainResultList).get();
                    insertDomainPool.submit(new InsertDomainThread(result.getDomain()));
                    indexInInsertDomainResultList++;
                    if (indexInInsertDomainResultList>=INSERT_DOMAIN_POOL_SIZE) indexInInsertDomainResultList=0;
                    break loop;
                  }
                  catch (InterruptedException | ExecutionException e) {
                    LOG.error("isDone",e);
                    //System.exit(100);
                  }
                }
              }
            }*/
            //DBFacade.saveOrUpdateDomain(result.getDomain());
            resultCounter++;
            if (requestCounter<=REQUEST_COUNT&&line!=null) {
              nsLookupResultList.set(i,nsLookupPool.submit(new NSLookupThread(getNextDomain(),timeout)));
              requestCounter++;
            }
            else {
              nsLookupResultList.remove(i);
              if (nsLookupResultList.size()==0) break;
            }
          } catch (InterruptedException | ExecutionException e) {
            LOG.error("isDone",e);
            //System.exit(100);
          }
        }
        if (++i>=nsLookupResultList.size()) i=0;
      }
    } catch (IOException e) {
      LOG.error("doThreadJob1",e);
    }
    finally {
      nsLookupPool.shutdown();
      insertDomainPool.shutdown();
    }
    LOG.warn("END1");
    while (!nsLookupPool.isTerminated())
      ;
    LOG.warn("END2");
    while (!insertDomainPool.isTerminated())
      ;
    LOG.warn("END3");
    BigDecimal totalTime=BigDecimal.valueOf((new Date().getTime()-startTime)/1000d).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    BigDecimal perSecond=BigDecimal.valueOf(requestCounter).divide(totalTime, 2, BigDecimal.ROUND_HALF_EVEN);
    LOG.warn("Total time:"+totalTime+" per sec: "+perSecond);
    //DBUtil.getSessionFactory().close();
  }
  

}
/*Attributes attrs = DNSLookup.reverseNsLookup("77.93.195.184", "8.8.8.8");
NamingEnumeration<? extends Attribute> enumAttrs = attrs.getAll();
while(enumAttrs.hasMoreElements()) {
  Attribute attr = enumAttrs.next();
  LOG.debug("  " + attr.getID() + " = " + attr.get());
}*/
