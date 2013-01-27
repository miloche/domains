package com.gmsxo.domains;

import java.io.IOException;

import com.gmsxo.domains.helpers.FileHelper;

public class CountLines {

  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    if (args.length!=1) {
      System.err.println("Usage CountLines fullPathToFile");
      System.exit(-1);
    }
    System.out.println(FileHelper.count(args[0]));
  }
}
