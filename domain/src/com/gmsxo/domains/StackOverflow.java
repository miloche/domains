package com.gmsxo.domains;

import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import java.util.Hashtable;

public final class StackOverflow {
  public static void main(String args[]) throws NamingException {
    Hashtable<String, Object> env = new Hashtable<String, Object>();
    env.put("java.naming.factory.initial", "com.gmsxo.domains.dns.dnsclient.DnsContextFactory");
    env.put("java.naming.provider.url", "dns://ns.dnssek.org");
    env.put("com.sun.jndi.dns.timeout.initial", "50000");
    env.put("com.sun.jndi.dns.recursion", "false");
    InitialDirContext id=new InitialDirContext(env);
    Attributes attrs = id.getAttributes("GOTANDA.us", new String[]{"A","NS"});
    System.out.println(attrs);
    
  }
}

//    env.put("com.sun.jndi.dns.timeout.initial", "220");
//    env.put("com.sun.jndi.dns.timeout.retries", "1");

