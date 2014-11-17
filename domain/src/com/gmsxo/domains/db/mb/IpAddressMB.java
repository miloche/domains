package com.gmsxo.domains.db.mb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
import com.gmsxo.domains.db.DBUtil;
import com.gmsxo.domains.helpers.Web;

@ViewScoped
@ManagedBean
public class IpAddressMB extends AbstractMB implements Serializable {
	private static final long serialVersionUID = 1L;

	//private static final int COLUMNS=3;
  private static final int ROWS=25;
  
  //private static final String ATTR_SELECTED_IP_ADDRESS="selectedIpAddress";
  private static final String ATTR_CENTER_COLUMN="centerColumn";
	
	private static Logger LOG=Logger.getLogger(IpAddressMB.class);
	
	//List<Domain> domainList0;
	//List<Domain> domainList1;
	//List<Domain> domainList2;
	
	
  public String moveLeft() {
    if (centerCol.prev==null) Column.setLeft(centerCol);
    centerCol=centerCol.prev;
    if (centerCol.prev==null) Column.setLeft(centerCol);
    return "success";
  }

  public String moveRight() {
    if (centerCol.next==null) Column.setRight(centerCol);
    centerCol=centerCol.next;
    if (centerCol.next==null) Column.setRight(centerCol);
    return "success";
  }
  
	//Map<String, Column> loadedIp = new HashMap<>();
	
	//Integer domainForIp;
	//Integer pageIp=0;
	
	//private String selectedIpAddress0="";
	//private String selectedIpAddress1="";
	//private String selectedIpAddress2="";
	
	//private IpAddress[] selAddress=new IpAddress[COLUMNS];

	//private int center=COLUMNS/2;
	private Column centerCol;
	
	public IpAddressMB() {
	  LOG.debug("IpAddressMB() constructor");
	  centerCol=(Column)Web.getAttribute(ATTR_CENTER_COLUMN);
	  LOG.debug("IpAddressMB() getAttribute() "+centerCol);
	  LOG.debug("IpAddressMB() Web.getRequestParam(\"ip\") "+Web.getRequestParam("ip"));
	  if (centerCol==null) {
	    centerCol=Column.getInstance(Web.getRequestParam("ip"));
	    Column.setLeft(centerCol);
	    Column.setRight(centerCol);
	    Web.setAttribute(ATTR_CENTER_COLUMN, centerCol);
	  }
	  LOG.debug("IpAddressMB() cetnerCol"+centerCol);
	  //init();
	}
	
	@SuppressWarnings("unchecked")
  /*private void init() {
	  LOG.debug("IpAddressMB.init()");
	  Session ses=DBUtil.openSession();
	  try {
  	  if (pageIp==null) {
  	    Query domainCountQuery
  	      =ses.createSQLQuery("select count(*) " +
  	    		"from domain_ip_address_lnk lnk,ip_address ip" +
  	    		" where lnk.ip_address_id=ip.id" +
  	    		" and ip.address='"+selAddress[center].getAddress()+"'");
  	    domainForIp=((BigInteger)domainCountQuery.uniqueResult()).intValue();
  	    pageIp=0;
  	    Web.setAttribute("domainForIp",domainForIp);
  	    Web.setAttribute("pageIp", pageIp);
  	  }
  	  
  	  if (domainForIp==null || domainForIp==0) {
  	    domainList1=new ArrayList<>();
  	    return;
  	  }
  	  
  	  Query domainQuery
  	    =ses.createSQLQuery("select d.id,d.name,null as ip_address_id " +
  	    		"from ip_address ip,domain_ip_address_lnk lnk,domain d " +
  	    		"where lnk.ip_address_id=ip.id" +
  	    		" and d.id=lnk.domain_id" +
  	    		" and ip.address='"+selAddress[center].getAddress()+"' offset "+(pageIp*ROWS) +" limit "+ROWS).addEntity(Domain.class);
  	  domainList1 = domainQuery.list();
	  }
	  finally{ses.close();}
	}*/
	
	/*private IpAddress findLeftIp(IpAddress ipAddress) {
    LOG.debug("IpAddressMB.findLeftIp(\""+ipAddress+"\")");
	  if (ipAddress==null) return null;
	  Session ses=DBUtil.openSession();
	  try {
	    return (IpAddress)ses.createSQLQuery("select * from ip_address where address<'"+ipAddress.getAddress()+"' order by address desc limit 1").addEntity(IpAddress.class).uniqueResult();
	  } finally {ses.close();}
	}
  private IpAddress findRightIp(IpAddress ipAddress) {
    LOG.debug("IpAddressMB.findLeftIp(\""+ipAddress+"\")");
    if (ipAddress==null) return null;
    Session ses=DBUtil.openSession();
    try {
      return (IpAddress)ses.createSQLQuery("select * from ip_address where address>'"+ipAddress.getAddress()+"' order by address asc limit 1").addEntity(IpAddress.class).uniqueResult();
    } finally {ses.close();}
  }*/

	/*public String inputIp() {
	  LOG.debug("inputIp:"+selAddress[center]);
	  //displayInfoMessageToUser("S:"+selectedDomainName);
    LOG.debug(selAddress[center]+" "+(String)Web.getAttribute(ATTR_SELECTED_IP_ADDRESS)+" "+selAddress[center].equals((IpAddress)Web.getAttribute(ATTR_SELECTED_IP_ADDRESS)));
    if (!selAddress[center].equals((String)Web.getAttribute(ATTR_SELECTED_IP_ADDRESS))) {
	    Web.setAttribute(ATTR_SELECTED_IP_ADDRESS, selAddress[center]);
	    Web.removeAttribute("pageIp");
	    init();
	  }
    return "success";
	}*/

  /*public IpAddress getSelectedIpAddress() {
    LOG.debug("getSelectedIpAddress:"+selAddress[center]);
    return centerCol.ipAddress;
  }

  public void setSelectedIpAddress(IpAddress selectedIpAddress) {
    LOG.debug("setSelectedIpAddress "+selectedIpAddress);
    this.selAddress[center] = selectedIpAddress;
  }*/
  
  public IpAddress getIpAddress0() {
    return centerCol.prev.ipAddress;
  }

  public IpAddress getIpAddress1() {
    return centerCol.ipAddress;
  }

  public IpAddress getIpAddress2() {
    return centerCol.next.ipAddress;
  }
  
   public List<Domain> getDomains0() {
    return centerCol.prev.domainList;
  }

  public List<Domain> getDomains1() {
    return centerCol.domainList;
  }
  public List<Domain> getDomains2() {
    return centerCol.next.domainList;
  }
  
  public boolean getUpHide0() {
    return centerCol.prev.page==0;
  }
  public boolean getDownHide0() {
    return (centerCol.prev.page+1)*ROWS>=centerCol.prev.domainCount;
  }
  public boolean getUpHide1() {
    return centerCol.page==0;
  }
  public boolean getDownHide1() {
    return (centerCol.page+1)*ROWS>=centerCol.domainCount;
  }
  public boolean getUpHide2() {
    return centerCol.next.page==0;
  }
  public boolean getDownHide2() {
    return (centerCol.next.page+1)*ROWS>=centerCol.next.domainCount;
  }
  
  public String down0() {
    LOG.debug("down10");
    centerCol.prev.down();
    return "success";
  }
  public String up0() {
    LOG.debug("up0");
    centerCol.prev.up();
    return "success";
  }
  public String down1() {
    LOG.debug("down1");
    centerCol.down();
    return "success";
  }
  public String up1() {
    LOG.debug("up1");
    centerCol.up();
    return "success";
  }
  public String down2() {
    LOG.debug("down2");
    centerCol.next.down();
    return "success";
  }
  public String up2() {
    LOG.debug("up2");
    centerCol.next.up();
    return "success";
  }

  /*public String right() {
    LOG.debug("right "+pageIp);
    pageIp++;
    Web.setAttribute("pageIp",pageIp);
    return "success";
  }
  
  public String left() {
    LOG.debug("left "+pageIp);
    pageIp--;
    Web.setAttribute("pageIp",pageIp);
    return "success";
  }*/
  public boolean getHideLeft() {
    //LOG.debug("getShowLeft "+pageIp+" "+(pageIp>0));
    //return pageIp==0;
    
    return false;
  }
  public boolean getHideRight() {
    //LOG.debug("getShowRight "+pageIp+" "+domainForIp+" "+ROWS+" "+((pageIp+1)*ROWS<domainForIp));
    //return (pageIp+1)*ROWS>=domainForIp;
    return false;
  }
  
  
  
  public static class Column implements Serializable{
    private static final Domain EMPTY=new Domain(".");
    /**
     * 
     */
    private static final long serialVersionUID = 5562497161559525563L;
    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Column [ipAddress=").append(ipAddress).append(", domainList=").append(domainList).append(", domainCount=")
          .append(domainCount).append(", page=").append(page).append(", prev=").append(prev!=null).append(", next=").append(next!=null).append("]");
      return builder.toString();
    }
    @SuppressWarnings("unchecked")
    static Column getInstance(String ipAddress) {
      Column column=new Column();
      
      Session ses=DBUtil.openSession(); try{
        column.ipAddress=(IpAddress)ses.createSQLQuery("select * from ip_address where address='"+ipAddress+"'").addEntity(IpAddress.class).uniqueResult();
        column.domainCount=((BigInteger)ses.createSQLQuery("select count(*) " +
            "from domain_ip_address_lnk lnk,ip_address ip where lnk.ip_address_id=ip.id and ip.address='"+ipAddress+"'").uniqueResult()).intValue();
        column.page=0;
        column.domainList=(List<Domain>)ses.createSQLQuery("select d.id,d.name,null as ip_address_id " +
            "from domain_ip_address_lnk lnk,domain d where lnk.ip_address_id="+column.ipAddress.getId()+" and d.id=lnk.domain_id limit "+ROWS)
            .addEntity(Domain.class).list();
        while (column.domainList.size()<ROWS) {
          column.domainList.add(EMPTY);
        }
        column.prev=null;
        column.next=null;
        return column;
      } finally {ses.close();}
    }
    static void setLeft(Column col) {
      Session ses=DBUtil.openSession();
      try {
        String leftIp=(String)ses.createSQLQuery("select address from ip_address where sort_address<'"+col.ipAddress.getSortAddress()+"' order by sort_address desc limit 1").uniqueResult();
        col.prev=null;
        if (leftIp==null) return;
        
        Column left=Column.getInstance(leftIp);
        left.next=col;
        col.prev=left;
      } finally {ses.close();}
    }
    static void setRight(Column col) {
      Session ses=DBUtil.openSession();
      try {
        String rightIp=(String)ses.createSQLQuery("select address from ip_address where sort_address>'"+col.ipAddress.getSortAddress()+"' order by sort_address asc limit 1").uniqueResult();
        col.next=null;
        if (rightIp==null) return;
        
        Column right=Column.getInstance(rightIp);
        right.prev=col;
        col.next=right;
      } finally {ses.close();}
    }
    
    @SuppressWarnings("unchecked")
    public void down() {
      LOG.debug("down "+this);
      page++;
      Session ses=DBUtil.openSession(); try{
        domainList=(List<Domain>)ses.createSQLQuery("select d.id,d.name,null as ip_address_id " +
            "from domain_ip_address_lnk lnk,domain d where lnk.ip_address_id="+ipAddress.getId()+" and d.id=lnk.domain_id offset "+(page*ROWS) +" limit "+ROWS)
            .addEntity(Domain.class).list();
        while (domainList.size()<ROWS) {
          domainList.add(EMPTY);
        }
      } finally {ses.close();}
    }

    @SuppressWarnings("unchecked")
    public void up() {
      LOG.debug("up "+this);
      page--;
      Session ses=DBUtil.openSession(); try{
        domainList=(List<Domain>)ses.createSQLQuery("select d.id,d.name,null as ip_address_id " +
            "from domain_ip_address_lnk lnk,domain d where lnk.ip_address_id="+ipAddress.getId()+" and d.id=lnk.domain_id offset "+(page*ROWS) +" limit "+ROWS)
            .addEntity(Domain.class).list();
        while (domainList.size()<ROWS) {
          domainList.add(EMPTY);
        }
      } finally {ses.close();}
    }

    public List<Domain> getDomainList() {
      return domainList;
    }
    public void setDomainList(List<Domain> domainList) {
      this.domainList=domainList;
    }

    IpAddress    ipAddress;
    List<Domain> domainList;
    Integer   domainCount;
    Integer   page;
    Column    prev;
    Column    next;
  }
}