package com.gmsxo.domains.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@Entity
@Table( name = "DNS_SERVER", uniqueConstraints = {@UniqueConstraint(columnNames="name", name="DOMAIN_NAME_CONS")}  )
@NamedQueries({
  @NamedQuery(name = "DnsServer.findById",   query = "SELECT a FROM DnsServer a WHERE a.id = :id"),
  @NamedQuery(name = "DnsServer.findByName", query = "SELECT a FROM DnsServer a WHERE a.name = :name")})
public class DnsServer implements Serializable, Comparable<DnsServer>, Insertable { private static final long serialVersionUID = -5122932828021960917L;

  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="DNSServerIdSeq")
  @SequenceGenerator( name = "DNSServerIdSeq", sequenceName="DNS_SERVER_ID_SEQ", allocationSize=1, initialValue=1)
  private Integer id;

  @Index(name="DNS_SERVER_NAME_IDX")
  private String name;
  
  @ManyToMany (mappedBy="dnsServer", cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch=FetchType.LAZY)
  private Set<Domain> domain=new HashSet<>();
  
  public DnsServer() {}
  public DnsServer(String name) { this.name=name; }
  public DnsServer(int id, String name) { this.id=id; this.name=name; }
  
  public Integer getId() { return id; }
  
  public DnsServer setId(Integer id) {
    this.id = id;
    return this;
  }
  public String getName() { return name; }
  
  public DnsServer setName(String name) {
    this.name = name;
    return this;
  }
  public Set<Domain> getDomain() { return domain; }
  
  public DnsServer setDomain(Set<Domain> domain) {
    this.domain = domain;
    return this;
  }

  @Override
  public int hashCode() { return 31 + ((getName() == null) ? 0 : getName().hashCode()); }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof DnsServer)) return false;
    DnsServer other = (DnsServer) obj;
    if (getName() == null) { if (other.getName() != null) return false; }
    else if (!getName().equals(other.getName())) return false;
    return true;
  }

  @Override
  public String toString() {return new StringBuilder("DNSServer [id=").append(id).append(", name=").append(name).append(", hashCode=").append(hashCode()).append("]").toString();}
  @Override
  public int compareTo(DnsServer o) {return this.getName().compareTo(o.getName());}
  @Override
  public String getQuery() { return String.format("select id from dns_server where name='%s'", getName()); }
  @Override
  public StringBuilder getInsertRoot() { return new StringBuilder("insert into dns_server (name) values "); }
  @Override
  public String getKeyValue() { return getName(); }
  @Override
  public void setInsertedId(int id) { setId(id); }
}
