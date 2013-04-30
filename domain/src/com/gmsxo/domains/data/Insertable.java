package com.gmsxo.domains.data;

public interface Insertable {
  String getQuery();
  StringBuilder getInsertRoot();
  String getKeyValue();
  void setInsertedId(int id);
  Integer getId();
}
