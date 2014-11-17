package com.gmsxo.domains.web.rewriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TryIt {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String subjectString="<x>asdfasdfdsfsda";

    Pattern regex = Pattern.compile("<x>(.*?)$", Pattern.DOTALL);
    Matcher matcher1 = regex.matcher(subjectString);
    System.out.println(matcher1.find());
    System.out.println(matcher1.group());
    System.out.println(matcher1.group(1));
    String test="/domain/domain/xxx.yyy.xxx";
    Pattern pattern=Pattern.compile("/domain/domain/(.*?)$");
    Matcher matcher=pattern.matcher(test);
    matcher.find();
    System.out.println("()  "+matcher.group());
    System.out.println("(0) "+matcher.group(0));
    System.out.println("(1) "+matcher.group(1));
    //System.out.println("(1) "+matcher.group(2));
    
    //for (String s:test.split("/domain/domain/(.*?)$")) System.out.println(s);
  }

}
