package com.gmsxo.domains.db.mb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.db.facade.DnsServerFacade;
import com.gmsxo.domains.db.facade.DomainFacade;
import com.gmsxo.domains.db.facade.IpAddressFacade;

@ViewScoped
@ManagedBean
public class DomainMB extends AbstractMB implements Serializable {
	private static final long serialVersionUID = 1L;

	//private static final String SELECTED_DOMAIN = "selectedDomain";
	
	private static Logger LOG=Logger.getLogger(DomainMB.class);

	private Domain domain;
	private DomainFacade domainFacade;
	private IpAddressFacade ipAddressFacade;
	private DnsServerFacade dnsServerFacade;
	
	List<DnsServer> dnsList;
	List<IpAddress> ipList;
	
	private String selectedDomainName="";
	
	public DomainMB() {
	  LOG.debug("DomainMB() constructor");
	   FacesContext context = FacesContext.getCurrentInstance();
	    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
	    String domainParam=request.getParameter("domain");
	    if (domainParam!=null) {
	      selectedDomainName=domainParam;
	    }
	    else {
	      selectedDomainName=(String)request.getSession().getAttribute("selectedDomain");
	    }
	    init();
	}

	public DomainFacade getDomainFacade() {
	  LOG.debug("getDomainFacade");
		if (domainFacade == null) domainFacade = new DomainFacade();
		return domainFacade;
	}
	
  public IpAddressFacade getIpAddressFacade() {
    LOG.debug("getIpAddressFacade");
    if (ipAddressFacade == null)ipAddressFacade = new IpAddressFacade();
    return ipAddressFacade;
  }

  public DnsServerFacade getDnsServerFacade() {
    LOG.debug("getDnsServerFacade");
    if (dnsServerFacade == null) dnsServerFacade = new DnsServerFacade();
    return dnsServerFacade;
  }

  public Domain getDomain() {
	  LOG.debug("getDomain");
	  if (domain==null) return new Domain();
	  if (selectedDomainName.equals(domain.getName())) return domain;
	  if (selectedDomainName!=null&&selectedDomainName.length()>0) {
	    domain=getDomainFacade().findByName(selectedDomainName);
	    return domain;
	  }
	  return new Domain();
	}

	public void setDomain(Domain domain) {
	  LOG.debug("setDomain("+domain+")");
		this.domain = domain;
	}
	
	@SuppressWarnings("unchecked")
  private void init() {
	  Session ses=DBUtil.openSession();
	  
	  Query domainQuery = ses.createSQLQuery("select id,name,null as ip_address_id from domain where name='"+selectedDomainName+"'").addEntity(Domain.class);
	  domain = (Domain)domainQuery.uniqueResult();
	  
	  domain=getDomainFacade().findByName(selectedDomainName);
	  
	  if (domain!=null) {
	    Query dnsQuery=ses.createSQLQuery("SELECT dns.* FROM dns_server dns, domain_dns_server_lnk lnk WHERE lnk.domain_id="+domain.getId()+" AND lnk.dns_server_id=dns.id")
	                      .addEntity(DnsServer.class);
	    dnsList=dnsQuery.list();
	    Query ipQuery=ses.createSQLQuery("SELECT a.* FROM ip_address a, domain_ip_address_lnk lnk WHERE lnk.domain_id = "+domain.getId()+" and lnk.ip_address_id=a.id")
	                     .addEntity(IpAddress.class);
	    IpAddress ip=(IpAddress)ipQuery.uniqueResult();
	    ipList=new ArrayList<>();
	    ipList.add(ip);
	  }
	  else
	   {
	    dnsList = new ArrayList<>();
	    ipList=new ArrayList<>();
	  }
	  
	  ses.close();
	}

	public String inputDomain() {
	  LOG.debug("inputDomain:"+selectedDomainName);
	  //displayInfoMessageToUser("S:"+selectedDomainName);
	  FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    LOG.debug(selectedDomainName+" "+(String)request.getSession().getAttribute("selectedDomain")+" "+selectedDomainName.equals((String)request.getSession().getAttribute("selectedDomain")));
    if (!selectedDomainName.equals((String)request.getSession().getAttribute("selectedDomain"))) {
	    request.getSession().setAttribute("selectedDomain", selectedDomainName);
	    //omain=getDomainFacade().findByName(selectedDomainName);
	    init();
	  }
	  //return "/index.xhtml";
    return "success";
	}

  public String getSelectedDomainName() {
    LOG.debug("getSelectedDomainName:"+selectedDomainName);
    return selectedDomainName;
  }

  public void setSelectedDomainName(String selectedDomainName) {
    LOG.debug("setSelectedDomainName "+selectedDomainName);
    this.selectedDomainName = selectedDomainName;
  }

  public List<IpAddress> getIpAddress() {
    LOG.debug("getIpAddress() "+domain);
    if (this.domain==null) return new ArrayList<>();
    return ipList;
  }
  
  public List<DnsServer> getDnsServer() {
    LOG.debug("getDnsServer() "+domain);
    if (this.domain==null) return new ArrayList<>();
    return dnsList;
  }
}