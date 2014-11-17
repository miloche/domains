package com.gmsxo.domains.db.dao;

import java.util.HashMap;
import java.util.Map;

import com.gmsxo.domains.data.Domain;

public class DomainDAO extends GenericDAOHibernate<Domain> {
	 
	private static final long serialVersionUID = 1L;

	public DomainDAO() {
        super(Domain.class);
    }
	
	public Domain findById(int id) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("id", id);

    return super.findOneResult(Domain.FIND_BY_ID, parameters);
  }
	 public Domain findByName(String name) {
	    String sqlQuery="select id,name,null as ip_address_id from domain where name='"+name+"'";
	    
	    return super.findOneByNativeQuery(sqlQuery, Domain.class);
	    
	    //Map<String, Object> parameters = new HashMap<String, Object>();
	    //parameters.put("name", name);

	    //return super.findOneResult(Domain.FIND_BY_NAME, parameters);
	  }

}