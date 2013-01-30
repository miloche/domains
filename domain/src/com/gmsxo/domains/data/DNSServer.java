package com.gmsxo.domains.data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

@Entity
@Table( name = "DNS_SERVER", uniqueConstraints = {@UniqueConstraint(columnNames="domainName", name="DOMAIN_NAME_CONS")}  )
public class DNSServer implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 5268378373876358885L;

  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="DNSServerIdSeq")
  @SequenceGenerator( name = "DNSServerIdSeq", sequenceName="DNS_SERVER_ID_SEQ", allocationSize=1, initialValue=1)
  private Long id;

  @Index(name="DNS_SERVER_DOMAIN_NAME_IDX")
  private String domainName;
  
  @ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE}, targetEntity=IPAddress.class )
  @JoinColumn(name="IP_ADDRESS_ID")
  @ForeignKey(name="FK_IP_ADDRESS_ID")
  @Index(name="DNS_IP_ADDRESS_ID_IDX")
  private IPAddress ipAddress;
  
  @ManyToMany (mappedBy="dnsServer")
  private List<Domain> domain=new LinkedList<>();

  public DNSServer() {}
  
  public DNSServer(String domainName) {
    this.domainName=domainName;
  }
  
  public Long getId() {
    return id;
  }
  public DNSServer setId(Long id) {
    this.id = id;
    return this;
  }
  public String getDomainName() {
    return domainName;
  }
  public DNSServer setDomainName(String domainName) {
    this.domainName = domainName;
    return this;
  }
  public IPAddress getIpAddress() {
    return ipAddress;
  }
  public DNSServer setIpAddress(IPAddress ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }
  public List<Domain> getDomain() {
    return domain;
  }
  public void setDomain(List<Domain> domain) {
    this.domain = domain;
  }

  @Override
  public int hashCode() {
    return 31 + ((getDomainName() == null) ? 0 : getDomainName().hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof DNSServer)) return false;
    DNSServer other = (DNSServer) obj;
    if (getDomainName() == null) { if (other.getDomainName() != null) return false; }
    else if (!getDomainName().equals(other.getDomainName())) return false;
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder("DNSServer [id=").append(id)
        .append(", domainName=").append(domainName)
        .append(", ipAddress=").append(ipAddress).append("]")
        .toString();
  }
}
