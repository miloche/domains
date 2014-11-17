package com.gmsxo.domains.resolve.v1;

import org.apache.log4j.Logger;

public class TryIt {
  private static Logger LOG=Logger.getLogger(TryIt.class);
  
  private static void sleep(long time) {try {Thread.sleep(time);} catch (InterruptedException e) {LOG.error("interrupted",e);}}
  
  public static void main(String[] args) {
    
    String str = "i am fine. You are fine";
    String findStr = "fine";
    int lastIndex = 0;
    int count =0;

    while(lastIndex != -1){

           lastIndex = str.indexOf(findStr,lastIndex);

           if( lastIndex != -1){
             LOG.info(lastIndex);
                 count ++;
                 lastIndex+=findStr.length();
          }
    }
    System.out.println(count);
    
    //String str="fine1fine";
    //String what="fine";
    //for (int i=0,j=0;i<str.length();i++,j++) if (str.toLowerCase().substring(i).startsWith(what)) {LOG.info(j);j=0;i+=what.length();};

    //String str="i am fine. You are Fine";
    String what="fine";
    StringBuilder sb=new StringBuilder(str.toLowerCase());
    for (int index=0;;) {
      sb.delete(0, (index=sb.indexOf(what))+what.length());
      if (index<0) break;
      LOG.info(index);
    }
    
    if (true) return;
    MyThread mt=new MyThread();
    mt.start();
    LOG.info("sleeping for 2 sec");
    sleep(2000); 
    LOG.info("set wait 10");
    mt.setWait(10);
    LOG.info("sleeping for 2 sec");
    sleep(2000);
    LOG.info("interrupt");
    mt.interrupt();
    LOG.info("sleeping for 2 sec");
    sleep(2000);

  }
  
}

class MyThread extends Thread {
  private static Logger LOG=Logger.getLogger(MyThread.class);
  private int wait=5;
  
  

  @Override
  public void run() {
    super.run();
    while (true) {
      int res;
      try {
        res = getRes();
      } catch (InterruptedException e) {
        if (isInterrupted()) LOG.info("Is Interrupted");
        LOG.error("interrupt",e);
        break;
      }
      LOG.info(res);
      if (isInterrupted()) {
        LOG.info("isInterrupted");
        break;
      }
    }
  }

  public synchronized void setWait(int w) {
    wait=w;
    notify();
  }

  public synchronized int getRes() throws InterruptedException {
    while (wait==0) wait();
    return --wait; 
  }
}
