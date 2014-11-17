package com.gmsxo.domains.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@Table(name="APP_CONFIG_DATA")
public class AppConfigData {
  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="AppConfigDataIdSeq")
  @SequenceGenerator( name = "AppConfigDataIdSeq", sequenceName="APP_CONFIG_DATA_ID_SEQ", allocationSize=1, initialValue=1)
  private int id;
  
  @ManyToOne
  @JoinColumn(name="app_class_id")
  private AppClass appClass;
  
  private String key;
  private String value;
  public int getId() {
    return id;
  }
  public void setId(int id) {
    this.id = id;
  }
  public AppClass getAppClass() {
    return appClass;
  }
  public void setAppClass(AppClass appClass) {
    this.appClass = appClass;
  }
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    final int HASH_PRIME=31;
    return HASH_PRIME * (HASH_PRIME + ((getKey() == null) ? 0 : getKey().hashCode())) + ((getAppClass() == null) ? 0 : getAppClass().hashCode());
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof AppConfigData)) return false;
    AppConfigData other = (AppConfigData) obj;
    if (getKey() == null) {
      if (other.getKey() != null) return false;
    } else if (!getKey().equals(other.getKey()))
      return false;
    else if (getValue() == null) {
      if (other.getValue() != null) return false;
    } else if (!getValue().equals(other.getValue()))
      return false;
    else if (getAppClass() == null) {
      if (other.getAppClass() != null) return false;
    } else if (!getAppClass().equals(other.getAppClass()))
      return false;
    return true;
  }
  @Override
  public String toString() {return new StringBuilder("ConfigData [id=").append(id).append(", key=").append(key).append(", value=").append(value).append("]").toString();}
}
