package com.gmsxo.domains.db.facade;

import java.io.Serializable;

import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.db.dao.DomainDAO;

public class DomainFacade implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private DomainDAO domainDAO = new DomainDAO();

	 /*public Domain findDomain(int id) {
	    //domainDAO.beginTransaction();
	    domainDAO.createEntityManager();
	    Domain domain = domainDAO.find(id);
	    domainDAO.closeTransaction();
	    return domain;
	  }*/

	public Domain findByName(String name) {
	  //domainDAO.beginTransaction();
	  domainDAO.createEntityManager();
	  Domain domain = domainDAO.findByName(name);
	  domainDAO.closeTransaction();
		return domain;
	}
}