package com.gmsxo.domains.data;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Index;

@Entity
@Table(name="APP_CLASS")
public class AppClass {
  
  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="AppClassIdSeq")
  @SequenceGenerator( name = "AppClassIdSeq", sequenceName="APP_CLASS_ID_SEQ", allocationSize=1, initialValue=1)
  private int id;
  
  @Index(name="config_class_class_name_idx")
  private String className;
  
  @OneToMany(mappedBy="appClass", fetch = FetchType.EAGER)
  @MapKey(name="key")
  private Map<String, AppConfigData> configData;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }
  
  public Map<String, AppConfigData> getConfigData() {
    return configData;
  }

  public void setConfigData(Map<String, AppConfigData> configData) {
    this.configData = configData;
  }
  public String getValue(String key) {
    return configData.get(key).getValue();
  }

  @Override
  public int hashCode() {
    final int HASH_PRIME=31;
    return HASH_PRIME + ((getClassName() == null) ? 0 : getClassName().hashCode());
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof AppClass)) return false;
    AppClass other = (AppClass) obj;
    if (getClassName() == null) {
      if (other.getClassName() != null) return false;
    } else if (!getClassName().equals(other.getClassName()))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder("ConfigClass [id=").append(id).append(", className=").append(className).append(", configData=").append(configData).append("]").toString();
  }
}
