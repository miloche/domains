package com.gmsxo.domains;

import org.apache.log4j.Logger;

public class HelloWorld {
  private static final Logger LOG = Logger.getLogger(HelloWorld.class);

  /**
   * @param args
   */
  public static void main(String[] args) {
    LOG.debug("Hello world!" + (args.length > 0? " " + args[0]:""));
  }
}
