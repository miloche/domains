package com.gmsxo.domains.controller;
public class TryIt {

  public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String str="com.gmsxo.domains.resolve.v1.IPResolver";

    System.out.println(str.substring(str.lastIndexOf('!')+1));
  }

}
