package com.gmsxo.domains.db.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.DnsServer;

public class DnsServerDAO extends GenericDAOHibernate<DnsServer> {
	 
	private static final long serialVersionUID = 1L;
	private static Logger LOG=Logger.getLogger(DnsServerDAO.class);
	
	public DnsServerDAO() {
        super(DnsServer.class);
    }
	
	public DnsServer findById(int id) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("id", id);

    return super.findOneResult(DnsServer.FIND_BY_ID, parameters);
  }
	 public DnsServer findByName(String name) {
	    Map<String, Object> parameters = new HashMap<String, Object>();
	    parameters.put("name", name);

	    return super.findOneResult(DnsServer.FIND_BY_NAME, parameters);
	  }
	 
	  public List<DnsServer> findByDomainId(int domainId) {
	    LOG.debug("findByDomainId("+domainId+")");
	    String nativeQuery="SELECT dns.* FROM dns_server dns, domain_dns_server_lnk lnk WHERE lnk.domain_id="+domainId+" AND lnk.dns_server_id=dns.id";
	    return super.findListByNativeQuery(nativeQuery, DnsServer.class);
	  }

}