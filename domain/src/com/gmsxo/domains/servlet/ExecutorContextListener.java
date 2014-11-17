package com.gmsxo.domains.servlet;

import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.gmsxo.domains.controller.ThreadController;
import com.gmsxo.domains.db.DBUtil;

public class ExecutorContextListener implements ServletContextListener {
  private static Logger LOG=Logger.getLogger(ExecutorContextListener.class);

  //private  ExecutorService executor;
  Thread t;

  public void contextInitialized(ServletContextEvent arg0) {
    LOG.info("ExecutorContextListener.contextInitialized");
    t=new Thread(new ThreadController());
    t.setName("Controller");
    t.setUncaughtExceptionHandler(new ThreadController.ExceptionHandler());
    t.start();
  }

  public void contextDestroyed(ServletContextEvent arg0) {
    LOG.info("ExecutorContextListener.contextDestroyed");
    if (t!=null)t.interrupt();
    try {
      Thread.sleep(80);
    } catch (InterruptedException e) {LOG.error("INTERRUPTED",e);}
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
    for(Thread t:threadArray) {
        LOG.debug(t.getName()+" "+t.getClass().getName());
        if(t.getName().startsWith("NSLKP") || t.getClass().getName().equals("sun.net.dns.ResolverConfigurationImpl$AddressChangeListener")) {
            LOG.warn("FORCE STOP:"+t.getName()+" "+t.getState()+" "+t.getClass().getName());
            synchronized(t) {
                t.interrupt();
                t.stop(); //don't complain, it works
            }
            LOG.warn("State after:"+t.getState());
        }
    }
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {LOG.error("INTERRUPTED",e);}
    DBUtil.close();
    LOG.info("ExecutorContextListener.contextDestroyed "+Runtime.getRuntime().freeMemory());
  }

}

