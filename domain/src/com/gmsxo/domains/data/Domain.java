package com.gmsxo.domains.data;

public class Domain {
  private int id;
  private String domainName;
  private IPAddress ipAddress = new IPAddress();

  public Domain() {}

  public int getId() {
    return id;
  }
  public void setId(int id) {
    this.id = id;
  }
  public String getDomainName() {
    return domainName;
  }
  public void setDomainName(String domainName) {
    this.domainName = domainName;
  }
  public Integer getIpAddressId() {
    if (ipAddress==null) return null;
    return ipAddress.getId();
  }
  public void setIpAddressId(Integer ipAddressId) {
    if (ipAddressId != null) this.ipAddress.setId(ipAddressId);
  }
  public void setIpAddress(String ipAddress) {
    this.ipAddress.setIpAddress(ipAddress);
  }
  public String getIpAddress() {
    if (ipAddress==null) return null;
    return ipAddress.getIpAddress();
  }
  public void setIPAddress(IPAddress ipAddress) {
    this.ipAddress = ipAddress;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Domain [id=").append(id).append(", domainName=").append(domainName).append(", ipAddressId=").append(ipAddress.getId())
        .append(", ipAddress=").append(ipAddress.getIpAddress()).append("]");
    return builder.toString();
  }

}
