package com.gmsxo.domains.helpers;

public class DNSHelper {
  public static final String DOMAIN_IN_IMPORT_REGEX="^[A-Z0-9]([A-Z0-9\\-\\.]*){1}( NS | IN NS ){1}[A-Z0-9]([A-Z0-9\\-\\.]*){1}$";
  public static final String DNS_CHECK_REGEX="^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$";
  public static final String IP_CHECK_REGEXP="^([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)";

}
