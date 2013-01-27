package com.gmsxo.domains.imports;

import java.util.concurrent.Callable;

import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.db.DBFacade;

public class InsertDomainThread implements Callable<Object> {
  private Domain domain;  public InsertDomainThread(Domain domain) {this.domain=domain;}
  @Override public Object call() throws Exception { DBFacade.saveOrUpdateDomain(domain);  return null; }
}
