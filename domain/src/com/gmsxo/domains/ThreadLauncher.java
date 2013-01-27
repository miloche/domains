package com.gmsxo.domains;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

class SimpleThread implements Callable<Object> {

  @Override
  public Object call() throws Exception {
    
    return null;
  }
}

public class ThreadLauncher {
  private static final Logger LOG=Logger.getLogger(ThreadLauncher.class);
  /**
   * @param numberOfThreads
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      new ThreadLauncher(20000).doJob();
      //System.err.println("USAGE: java ThreadLauncher numberOfThreads");
      //System.exit(-1);
    }
    else {
      new ThreadLauncher(Integer.parseInt(args[0]));
    }
  }
  
  private final int numberOfThreads;
  
  public ThreadLauncher(int numberOfThreads) {
    this.numberOfThreads=numberOfThreads;
  }
  
  public void doJob() {
    long startTime=new Date().getTime();
    ExecutorService pool=Executors.newFixedThreadPool(numberOfThreads);
    List<Future<Object>> resultList = new LinkedList<>();
    for (int i=0;i<numberOfThreads;i++) resultList.add(pool.submit(new SimpleThread()));
    pool.shutdown();
    while (!pool.isTerminated())
      ;
    LOG.info("Time: "+(new Date().getTime()-startTime)/1000+ " sec");
  }
}
