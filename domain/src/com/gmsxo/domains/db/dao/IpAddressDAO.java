package com.gmsxo.domains.db.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.IpAddress;

public class IpAddressDAO extends GenericDAOHibernate<IpAddress> {
	 
	private static final long serialVersionUID = 1L;
	private static final Logger LOG=Logger.getLogger(IpAddressDAO.class);

	public IpAddressDAO() {
        super(IpAddress.class);
    }
	
	public IpAddress findById(int id) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("id", id);

    return super.findOneResult(IpAddress.FIND_BY_ID, parameters);
  }
	public IpAddress findByAddress(String address) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("address", address);

    return super.findOneResult(IpAddress.FIND_BY_ADDRESS, parameters);
  }
	
  public IpAddress findByDomainId(int domainId) {
    LOG.debug("findByDomainId("+domainId+")");
    String nativeQuery="SELECT a.* FROM ip_address a, domain_ip_address_lnk lnk WHERE lnk.domain_id = "+domainId+" and lnk.ip_address_id=a.id";
    return super.findOneByNativeQuery(nativeQuery, IpAddress.class);
  }

}