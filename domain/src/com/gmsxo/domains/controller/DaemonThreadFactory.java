package com.gmsxo.domains.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {

  private final ThreadFactory factory;

  /**
   * Construct a ThreadFactory with setDeamon(true) using
   * Executors.defaultThreadFactory()
   */
  public DaemonThreadFactory() {
    this(Executors.defaultThreadFactory());
  }

  /**
   * Construct a ThreadFactory with setDeamon(true) wrapping the given factory
   * 
   * @param thread factory to wrap
   */
  public DaemonThreadFactory(ThreadFactory factory) {
    if (factory == null)
      throw new NullPointerException("factory cannot be null");
    this.factory = factory;
  }

  public Thread newThread(Runnable r) {
    final Thread t = factory.newThread(r);
    t.setDaemon(true);
    return t;
  }
}