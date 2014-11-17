package com.gmsxo.domains.db.facade;

import java.io.Serializable;
import java.util.List;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.db.dao.DnsServerDAO;

public class DnsServerFacade implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private DnsServerDAO dnsServerDAO = new DnsServerDAO();

	 /*public DnsServer finDnsServer(int id) {
	    dnsServerDAO.createEntityManager();
	    DnsServer dnsServer = dnsServerDAO.find(id);
	    dnsServerDAO.closeTransaction();
	    return dnsServer;
	  }*/

	/*public DnsServer findByName(String name) {
	  dnsServerDAO.createEntityManager();
	  DnsServer dnsServer = dnsServerDAO.findByName(name);
	  dnsServerDAO.closeTransaction();
		return dnsServer;
	}*/
	
	public List<DnsServer> findByDomainId(int domainId) {
	  dnsServerDAO.createEntityManager();
	  List<DnsServer> dnsList = dnsServerDAO.findByDomainId(domainId);
	  dnsServerDAO.closeTransaction();
    return dnsList;
  }
}