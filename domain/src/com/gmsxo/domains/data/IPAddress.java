package com.gmsxo.domains.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

@Entity
@Table(name = "IP_ADDRESS", uniqueConstraints = {@UniqueConstraint(columnNames="ipAddress", name="IP_ADDRESS_CONS")})
public class IPAddress implements Serializable { private static final long serialVersionUID = -8238580043625483358L;
public static final Map<String,String> errorMap=new HashMap<>();
static {
  errorMap.put("NO IP Address", "NO_IP_ADDRESS");
  errorMap.put("Unknown DNS server", "UNKNOWN_DNS_SERVER");
  errorMap.put("DNS service refused", "DNS_SERVICE_REFUSED");
  errorMap.put("DNS server failure", "DNS_SERVER_FAILURE");
  errorMap.put("DNS name not found", "DNS_NAME_NOT_FOUND");
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
  private Long id;
  
  @Index(name="IP_ADDRESS_IP_ADDRESS_IDX")
  private String ipAddress;
  
  @OneToMany( mappedBy="ipAddress")
  private List<Domain> domain=new LinkedList<>();

  public IPAddress() {
    super();
  }
  public IPAddress(Long id) {
    this.id = id;
  }
  public IPAddress(String ipAddress) {
    super();
    this.ipAddress = ipAddress;
  }
  public IPAddress(Long id, String ipAddress) {
    super();
    this.id = id;
    this.ipAddress = ipAddress;
  }

  public Long getId() {
    return id;
  }
  public IPAddress setId(Long id) {
    this.id = id;
    return this;
  }
  public String getIpAddress() {
    return ipAddress;
  }
  public IPAddress setIpAddress(String ipAddress) {
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
    return 31 + ((ipAddress == null) ? 0 : ipAddress.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof IPAddress)) return false;
    IPAddress other = (IPAddress) obj;
    if (getIpAddress() == null) {
      if (other.getIpAddress() != null) return false;
    }
    else if (!getIpAddress().equals(other.getIpAddress())) return false;
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder("IPAddress [id=").append(id).append(", ipAddress=").append(ipAddress).append("]").toString();
  }
}
