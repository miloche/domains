package com.gmsxo.domains;

import java.io.IOException;

import com.gmsxo.domains.helpers.FileHelper;

public class CutFileIntoParts {

  /**
   * @param args
   * @throws IOException 
   * @throws NumberFormatException 
   */
  public static void main(String[] args) throws NumberFormatException, IOException {
    if (args.length!=5) {
      System.err.println("Usage CutFileIntoParts inputDir\\ fileName outputDir\\ numberOfParts appendix");
      System.exit(-1);
    }
    FileHelper.cutFileIntoParts(args[0], args[1], args[2], Integer.parseInt(args[3]), args[4]);
  }

}
