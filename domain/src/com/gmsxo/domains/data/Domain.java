package com.gmsxo.domains.data;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@Entity
@Table(name="DOMAIN")
@SQLInsert(sql="INSERT INTO domain (name, id) VALUES (?, ?)",check=ResultCheckStyle.NONE)
@NamedQueries({
  @NamedQuery(name = "Domain.findById",   query = "SELECT a FROM Domain a WHERE a.id = :id"),
  @NamedQuery(name = "Domain.findByName", query = "SELECT a FROM Domain a WHERE a.name = :name")})
public class Domain implements Serializable, Comparable<Domain> { private static final long serialVersionUID = -8911470326504547499L;

  public static final String FIND_BY_ID="Domain.findById";
  public static final String FIND_BY_NAME="Domain.findByName";


  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="DomainIdSeq")
  @SequenceGenerator( name = "DomainIdSeq", sequenceName="DOMAIN_ID_SEQ", allocationSize=1, initialValue=1)
  private int id;
  
  @Index(name="DOMAIN_NAME_IDX")
  private String name;
  
  @ManyToOne(targetEntity=IpAddress.class, cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch=FetchType.LAZY)
  @JoinTable(name="DOMAIN_IP_ADDRESS_LNK", joinColumns=@JoinColumn(name="DOMAIN_ID"),inverseJoinColumns=@JoinColumn(name="IP_ADDRESS_ID"))
  private IpAddress ipAddress;
  
  @ManyToMany(targetEntity=DnsServer.class, cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch=FetchType.LAZY)
  @JoinTable(name="DOMAIN_DNS_SERVER_LNK", joinColumns=@JoinColumn(name="DOMAIN_ID"),inverseJoinColumns=@JoinColumn(name="DNS_SERVER_ID"))
  private Set<DnsServer> dnsServer = new TreeSet<>();
  
  public Domain() { super(); }

  public Domain(int id) {
    super();
    this.id = id;
  }
  public Domain(String name) {
    super();
    this.name = name;
  }
  public Domain(int id, String name) {
    super();
    this.id = id;
    this.name = name;
  }
  public int getId() { return id; }
  public Domain setId(final int id) {
    this.id = id;
    return this;
  }
  public String getName() { return name; }
  public Domain setName(final String name) {
    this.name = name;
    return this;
  }
  public IpAddress getIpAddress() {
    return ipAddress;
  }
  public Domain setIpAddress(IpAddress ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }
  public Set<DnsServer> getDnsServer() {
    return dnsServer;
  }
  public Domain setDnsServer(Set<DnsServer> dnsServer) {
    this.dnsServer = dnsServer;
    return this;
  }
  public Domain addDnsServer(DnsServer dnsServer) {
    this.dnsServer.add(dnsServer);
    return this;
  }
  
  public String toStringFull() {
    return new StringBuilder("Domain [id=").append(getId())
        .append(", name=").append(getName())
        .append(", ").append(getIpAddress())
        .append(", ").append(getDnsServer())
        .append("]").toString(); 
  }
  
  @Override
  public int hashCode() {
    return 31 + ((getName() == null) ? 0 : getName().hashCode());
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof Domain)) return false;
    Domain other = (Domain) obj;
    if (getName() == null) {
      if (other.getName() != null) return false;
    } else if (!getName().equals(other.getName()))
      return false;
    return true;
  }

  @Override
  public String toString() { return new StringBuilder("Domain [id=").append(id).append(", name=").append(name).append("]").toString(); }

  @Override
  public int compareTo(Domain o) { return getName().compareTo(o.getName()); }
}
