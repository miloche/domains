package com.gmsxo.domains.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@Entity
@Table(name = "IP_ADDRESS", uniqueConstraints = {@UniqueConstraint(columnNames="address", name="ADDRESS_CONS")})
@NamedQueries({
  @NamedQuery(name = "IpAddress.findById",   query = "SELECT a FROM IpAddress a WHERE a.id = :id"),
  @NamedQuery(name = "IpAddress.findByAddress", query = "SELECT a FROM IpAddress a WHERE a.address = :address")})

public class IpAddress implements Serializable, Comparable<IpAddress>, Insertable { private static final long serialVersionUID = -8796089977877378920L;

  public static final Map<String,String> errorMap=new HashMap<>();
  static {
    errorMap.put("NO IP Address", "NO_IP_ADDRESS");
    errorMap.put("Unknown DNS server", "UNKNOWN_DNS_SER");
    errorMap.put("DNS service refused", "DNS_SERVICE_REF");
    errorMap.put("DNS server failure", "DNS_SERVER_FAIL");
    errorMap.put("DNS name not found", "DNS_NAME_NOT_FO");
    errorMap.put("DNS error", "DNS_ERROR");
    errorMap.put("NULL IP", "NULL_IP");
    errorMap.put("ERROR", "ERROR");
  }
  public static final String[] errors = new String[]
      { "NO IP Address",
        "Unknown DNS server",
         "DNS service refused",
         "DNS server failure",
         "DNS name not found",
         "DNS error",
         "NULL IP",
         "ERROR"};

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IPAddressIdSeq")
    @SequenceGenerator(name = "IPAddressIdSeq", sequenceName = "IP_ADDRESS_ID_SEQ", allocationSize = 1, initialValue = 100)
    private Integer id;
    
    @Index(name="IP_ADDRESS_ADDRESS_IDX")
    private String address;
    
    @OneToMany( fetch=FetchType.LAZY)
    @JoinTable(name="DOMAIN_IP_ADDRESS_LNK", joinColumns=@JoinColumn(name="DOMAIN_ID"),inverseJoinColumns=@JoinColumn(name="IP_ADDRESS_ID"))
    private List<Domain> domain=new LinkedList<>();

    public IpAddress() {
      super();
    }
    public IpAddress(Integer id) {
      this.id = id;
    }
    public IpAddress(String ipAddress) {
      super();
      this.address = ipAddress;
    }
    public IpAddress(Integer id, String ipAddress) {
      super();
      this.id = id;
      this.address = ipAddress;
    }

    public Integer getId() {
      return id;
    }
    public IpAddress setId(Integer id) {
      this.id = id;
      return this;
    }
    public String getAddress() {
      return address;
    }
    public IpAddress setAddress(String address) {
      this.address = address;
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
      return 31 + ((address == null) ? 0 : address.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (!(obj instanceof IpAddress)) return false;
      IpAddress other = (IpAddress) obj;
      if (getAddress() == null) {
        if (other.getAddress() != null) return false;
      }
      else if (!getAddress().equals(other.getAddress())) return false;
      return true;
    }

    @Override
    public String toString() { return new StringBuilder("IPAddress [id=").append(getId()).append(", address=").append(getAddress()).append("]").toString(); }
    @Override
    public int compareTo(IpAddress o) { return this.getAddress().compareTo(o.getAddress())*(-1); } // without -1 a treeset has reverse order
    @Override
    public String getQuery() { return String.format("select id from ip_address where address='%s'", getAddress().substring(0, Math.min(getAddress().length(), 15))); }
    @Override
    public StringBuilder getInsertRoot() { return new StringBuilder("insert into ip_address (address) values "); }
    @Override
    public String getKeyValue() { return getAddress(); }
    @Override
    public void setInsertedId(int id) { setId(id); }
}
