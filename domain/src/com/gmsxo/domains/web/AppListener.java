package com.gmsxo.domains.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.gmsxo.domains.db.DBUtil;

public class AppListener implements ServletContextListener {

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
    //DBUtil.close();

  }

  @Override
  public void contextInitialized(ServletContextEvent arg0) {
    // TODO Auto-generated method stub

  }

}
