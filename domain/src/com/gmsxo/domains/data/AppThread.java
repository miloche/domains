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
@Table(name="APP_THREAD")
public class AppThread {
  
  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="AppThreadIdSeq")
  @SequenceGenerator( name = "AppThreadIdSeq", sequenceName="APP_THREAD_ID_SEQ", allocationSize=1, initialValue=1)
  private int id;
  
  @ManyToOne
  @JoinColumn(name="app_class_id")
  private AppClass appClass;
  
  private Status status;

  public void setId(int id) {
    this.id = id;
  }
  public AppClass getAppClass() {
    return appClass;
  }
  public void setAppClass(AppClass appClass) {
    this.appClass = appClass;
  }
  public Status getStatus() {
    return status;
  }
  public void setStatus(Status status) {
    this.status = status;
  }
  @Override
  public int hashCode() {
    final int HASH_PRIME=31;
    return HASH_PRIME + ((getAppClass() == null) ? 0 : getAppClass().hashCode());
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof AppThread)) return false;
    AppThread other = (AppThread) obj;
    if (getAppClass() == null) {
      if (other.getAppClass() != null) return false;
    } else if (!getAppClass().equals(other.getAppClass()))
      return false;
    return true;
  }
  @Override public String toString() {return new StringBuilder("AppThread [id=").append(id).append(", appClass=").append(appClass).append(", status=").append(status).append("]").toString();}
  
  public enum Status { Stopped (0), Running (1);
  @SuppressWarnings("unused") private int value;
  private Status(int value) {this.value=value;}
}

}
