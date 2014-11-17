package com.gmsxo.domains.db.facade;

import java.io.Serializable;

import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.dao.IpAddressDAO;

public class IpAddressFacade implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private IpAddressDAO ipAddressDAO = new IpAddressDAO();

	 /*public IpAddress findIpAddress(int id) {
	    ipAddressDAO.createEntityManager();
	    IpAddress ipAddress = ipAddressDAO.find(id);
	    ipAddressDAO.closeTransaction();
	    return ipAddress;
	  }*/

	public IpAddress findByAddress(String address) {
	  ipAddressDAO.createEntityManager();
	  IpAddress ipAddress = ipAddressDAO.findByAddress(address);
	  ipAddressDAO.closeTransaction();
		return ipAddress;
	}
	
	public IpAddress findByDomainId(int domainId) {
	  ipAddressDAO.createEntityManager();
    IpAddress ipAddress = ipAddressDAO.findByDomainId(domainId);
    ipAddressDAO.closeTransaction();
    return ipAddress;
	}
}