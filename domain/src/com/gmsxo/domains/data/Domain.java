package com.gmsxo.domains.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType; 
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

@Entity
@Table(name="DOMAIN", uniqueConstraints = { @UniqueConstraint(columnNames = "domainName", name="DOMAIN_NAME_CONS") })
public class Domain implements Serializable { private static final long serialVersionUID = -2768332354837953977L;

  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="DomainIdSeq")
  @SequenceGenerator( name = "DomainIdSeq", sequenceName="DOMAIN_ID_SEQ", allocationSize=1, initialValue=1)
  private Long id;

  @Index(name="DOMAIN_DOMAIN_NAME_IDX")
  private String domainName;

  @ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE}, targetEntity=IPAddress.class )
  @JoinColumn(name="IP_ADDRESS_ID")
  @ForeignKey(name="FK_IP_ADDRESS_ID")
  @Index(name="DOMAIN_IP_ADDRESS_ID_IDX", columnNames="IP_ADDRESS_ID")
  private IPAddress ipAddress = new IPAddress();

  @ManyToMany(targetEntity=DNSServer.class, cascade={CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(name="DOMAIN_DNS_SERVER_LNK", joinColumns=@JoinColumn(name="DOMAIN_ID"),inverseJoinColumns=@JoinColumn(name="DNS_SERVER_ID"))//,
     //uniqueConstraints = { @UniqueConstraint(columnNames = {"DOMAIN_ID", "DNS_SERVER_ID"}, name="domain_dns_server_lnk_key")})
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="DomainDNSServerLnkIdSeq")
  @SequenceGenerator(name = "DomainDNSServerLnkIdSeq", sequenceName="DOMAIN_DNS_SERVER_LNK_ID_SEQ", allocationSize=1, initialValue=1)
  @CollectionId(columns = @Column(name="ID"), type=@Type(type="long"), generator = "DomainDNSServerLnkIdSeq")
  @ForeignKey(name="DOMAIN_ID_CONS", inverseName="DNS_SERVER_ID_CONS")
  private List<DNSServer> dnsServer = new ArrayList<>();

  public Domain() {
    super();
  }
  public Domain(String domainName) {
    super();
    this.domainName=domainName;
  }
  public Domain(String domainName, IPAddress ipAddress) {
    this(domainName);
    this.ipAddress=ipAddress;
  }
  public Long getId() {
    return id;
  }
  public Domain setId(Long id) {
    this.id = id;
    return this;
  }
  public String getDomainName() {
    return domainName;
  }
  public Domain setDomainName(String domainName) {
    this.domainName = domainName;
    return this;
  }
  public IPAddress getIpAddress() {
    if (ipAddress==null) return null;
    return ipAddress;
  }
  public Domain setIPAddress(IPAddress ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }
  public List<DNSServer> getDnsServer() {
    return dnsServer;
  }
  public Domain setDnsServer(List<DNSServer> dnsServer) {
    if (dnsServer==null) this.dnsServer=new LinkedList<>();
    else this.dnsServer = dnsServer;
    return this;
  }
  public Domain addDnsServer(DNSServer dnsServer) {
    this.dnsServer.add(dnsServer);
    return this;
  }

  @Override
  public int hashCode() {
    return 31 + ((getDomainName() == null) ? 0 : getDomainName().hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof Domain)) return false;
    Domain other = (Domain) obj;
    if (getDomainName() == null) {
      if (other.getDomainName() != null) return false;
    } else if (!getDomainName().equals(other.getDomainName()))
      return false;
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Domain [id=").append(id)
           .append(", domainName=").append(domainName)
           .append(", ipAddressId=").append(ipAddress==null?null:ipAddress.getId())
           .append(", ipAddress=").append(ipAddress==null?null:ipAddress.getIpAddress());
    builder.append(" [");
    if (dnsServer==null) builder.append("null");
    else for (DNSServer server : dnsServer) builder.append(server.toString());
    builder.append("]]");
    return builder.toString();
  }
}
