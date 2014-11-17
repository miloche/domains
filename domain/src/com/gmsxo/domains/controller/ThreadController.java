package com.gmsxo.domains.controller;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.AppThread;
import com.gmsxo.domains.db.DBUtil;

/**
 * Manages background threads.
 * 
 * @author miloxe
 *
 */
public class ThreadController implements Runnable {
  private static final Logger LOG=Logger.getLogger(ThreadController.class);
  private static final char CLASS_DELIMITER='.';
  private static final int  DELAY=5000;
  private Map<AppThread,Thread> runningThreads=new HashMap<>();
  /**
   * Periodically starts and stops threads according to the data loaded from database.
   */
  @Override public void run() {
    try {
      LOG.info("Thread controller started");
      while (true) {
        if (Thread.interrupted()) {
          stopRunningThreads();
          LOG.info("Thread controller interrupted");
          return;
        }
        List<AppThread> threads=loadAppThreads();
        startRunningThreads(threads);
        stopStoppedThreads(threads);
        // start running threads
       /* List<AppThread> threads=loadAppThreads();
        for (AppThread appThread:threads) {
          LOG.debug(appThread);
          if (appThread.getStatus()==AppThread.Status.Running) {
            if (!runningThreads.containsKey(appThread)) { 
              Thread thread=startThread(appThread);
              thread.setName(appThread.getAppClass().getClassName().substring(appThread.getAppClass().getClassName().lastIndexOf(CLASS_DELIMITER)+1));
              if (thread!=null) runningThreads.put(appThread, thread);
            }
          }
        }*/
        // stop stopped threads
        /*for (Iterator<Entry<AppThread, Thread>> it=runningThreads.entrySet().iterator();it.hasNext();) {
          Map.Entry<AppThread, Thread> entry=it.next();
          int index=threads.indexOf(entry.getKey());
          AppThread appThread=null;
          if (index>=0) appThread=threads.get(index);
          if (index<0||appThread.getStatus()==AppThread.Status.Stopped) {
            entry.getValue().interrupt();
            it.remove();
          }
        }*/
        Thread.sleep(DELAY);
      }
    } catch (InterruptedException e) {
      stopRunningThreads();
      LOG.error("interrupted",e);
      Thread.currentThread().interrupt();
      return;
    }
      catch (Exception e) {LOG.error("exception",e);}
      finally {LOG.info("Thread controller finished");}
  }
  /**
   * Compares list of loaded threads with actually running threads and starts all which aren't started.
   * 
   * @param threads
   */
  private void startRunningThreads(List<AppThread> threads) {
    // start running threads
    //List<AppThread> threads=loadAppThreads();
    for (AppThread appThread:threads) {
      LOG.debug(appThread);
      if (appThread.getStatus()==AppThread.Status.Running) {
        if (!runningThreads.containsKey(appThread)) { 
          Thread thread=startThread(appThread);
          thread.setName(appThread.getAppClass().getClassName().substring(appThread.getAppClass().getClassName().lastIndexOf(CLASS_DELIMITER)+1));
          if (thread!=null) runningThreads.put(appThread, thread);
        }
      }
    }
  }
  /**
   * Compares list of loaded threads with actually running threads and stops all which are stopped.
   * 
   * @param threads
   */
  private void stopStoppedThreads(List<AppThread> threads) {
    for (Iterator<Entry<AppThread, Thread>> it=runningThreads.entrySet().iterator();it.hasNext();) {
      Map.Entry<AppThread, Thread> entry=it.next();
      int index=threads.indexOf(entry.getKey());
      AppThread appThread=null;
      if (index>=0) appThread=threads.get(index);
      if (index<0||appThread.getStatus()==AppThread.Status.Stopped) {
        entry.getValue().interrupt();
        it.remove();
      }
    }
  }
  private void stopRunningThreads() {
    LOG.debug("Stopping threads");
    for (Thread t:runningThreads.values()) {
      LOG.debug("Stopping "+t.getName());
      t.interrupt();
    }
    LOG.debug("Stopping threads end");
  }
  /**
   * Starts a given thread and sets its name.
   * 
   * @param appThread
   * @return
   */
  private Thread startThread(AppThread appThread) {
    Thread thread=null;
    try {
      thread=new Thread((Runnable)Class.forName(appThread.getAppClass().getClassName()).newInstance());
      thread.start();
    }
    catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {LOG.error("Thread failed to start:"+appThread,e);}
    return thread;
  }
  /**
   * Loads threads from database.
   * 
   * @return
   */
  @SuppressWarnings("unchecked") private List<AppThread> loadAppThreads() {
    List<AppThread> threads;
    Session ses=DBUtil.openSession();
    threads=(List<AppThread>)ses.createQuery("from AppThread").list();
    ses.close();
    return threads;
  }
  public static class ExceptionHandler implements UncaughtExceptionHandler {public void uncaughtException(Thread t, Throwable e) {LOG.error("uncaught",e);}}
}
