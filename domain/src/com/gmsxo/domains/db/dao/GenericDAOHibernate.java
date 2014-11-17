package com.gmsxo.domains.db.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//import javax.persistence.EntityManager;
//import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
//import javax.persistence.Persistence;
//import javax.persistence.Query;
//import javax.persistence.criteria.CriteriaQuery;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Query;

import com.gmsxo.domains.db.DBUtil;

abstract class GenericDAOHibernate<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG=Logger.getLogger(GenericDAOHibernate.class);

	//private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("JSFCrudPU");
	//private EntityManager em;
	
	private Session ses;

	private Class<T> entityClass;
	
	public void createEntityManager() {
    LOG.debug("createEntityManager");
    //em = emf.createEntityManager();
    ses = DBUtil.openSession();
	  
	}

	public void beginTransaction() {
	  LOG.debug("beginTransaction");
		//em = emf.createEntityManager();

		//em.getTransaction().begin();
	  ses.beginTransaction();
	}

	public void commit() {
	  LOG.debug("commit");
		//em.getTransaction().commit();
	  ses.getTransaction().commit();
	}

	public void rollback() {
	  LOG.debug("rollback");
		//em.getTransaction().rollback();
	  ses.getTransaction().rollback();
	}

	public void closeTransaction() {
	  LOG.debug("closeTransaction");
		//em.close();
	  ses.close();
	}

	public void commitAndCloseTransaction() {
	  LOG.debug("commitAndCloseTransaction");
		commit();
		closeTransaction();
	}

	public void flush() {
	  LOG.debug("flush");
		//em.flush();
	  ses.flush();
	}

	/*public void joinTransaction() {
	  LOG.debug("joinTransaction");
		em = emf.createEntityManager();
		em.joinTransaction();
	}*/

	public GenericDAOHibernate(Class<T> entityClass) {
	  LOG.debug("GenericDAO "+entityClass+" "+entityClass.getClass());
		this.entityClass = entityClass;
	}

	public void save(T entity) {
	  LOG.debug("save");
		//em.persist(entity);
	  ses.save(entity);
	}

	public void delete(T entity) {
	  LOG.debug("delete");
		//T entityToBeRemoved = em.merge(entity);

		//em.remove(entityToBeRemoved);
		
		ses.delete(entity);
	}

	@SuppressWarnings("unchecked")
  public T update(T entity) {
	  LOG.debug("update");
		//return em.merge(entity);
	  return (T)ses.merge(entity);
	}

	/*public T find(int entityID) {
	  LOG.debug("find");
		return em.find(entityClass, entityID);
	}*/

	/*public T findReferenceOnly(int entityID) {
	  LOG.debug("findReferenceOnly");
		return em.getReference(entityClass, entityID);
	}*/

	// Using the unchecked because JPA does not have a
	// em.getCriteriaBuilder().createQuery()<T> method
	/*@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<T> findAll() {
	  LOG.debug("findAll");
		CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
		cq.select(cq.from(entityClass));
		return em.createQuery(cq).getResultList();
	}*/

	// Using the unchecked because JPA does not have a
	// query.getSingleResult()<T> method
	@SuppressWarnings("unchecked")
	protected T findOneResult(String namedQuery, Map<String, Object> parameters) {
	  LOG.debug("findOneResult: "+namedQuery+" "+parameters);
		T result = null;

		try {
			//Query query = em.createNamedQuery(namedQuery);
		  Query query = ses.getNamedQuery(namedQuery);
			
			// Method that will populate parameters if they are passed not null and empty
			if (parameters != null && !parameters.isEmpty()) {
				populateQueryParameters(query, parameters);
			}

			result = (T) query.uniqueResult();

		} catch (NoResultException e) {
			System.out.println("No result found for named query: " + namedQuery);
		} catch (Exception e) {
			System.out.println("Error while running query: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
		  
		}

		return result;
	}
	
	@SuppressWarnings("unchecked")
  protected T findOneByNativeQuery(String nativeQuery, Class<T> type) {
	   LOG.debug("findOneByNativeQuery: "+nativeQuery);
	    T result = null;

	    try {
	      org.hibernate.Query query = ses.createSQLQuery(nativeQuery).addEntity(type);

	      result = (T) query.uniqueResult();

	    } catch (NoResultException e) {
	      System.out.println("No result found for named query: " + nativeQuery);
	    } catch (Exception e) {
	      System.out.println("Error while running query: " + e.getMessage());
	      e.printStackTrace();
	    }
	    finally {
	      
	    }

	    return result;
	}

	 @SuppressWarnings("unchecked")
	  protected List<T> findListByNativeQuery(String nativeQuery, Class<T> type) {
	     LOG.debug("findOneByNativeQuery: "+nativeQuery);
	     List<T> result = null;

	      try {
	        org.hibernate.Query query = ses.createSQLQuery(nativeQuery).addEntity(type);

	        result = (List<T>)query.list();

	      } catch (NoResultException e) {
	        System.out.println("No result found for named query: " + nativeQuery);
	      } catch (Exception e) {
	        System.out.println("Error while running query: " + e.getMessage());
	        e.printStackTrace();
	      }
	      finally {
	        
	      }

	      return result;
	  }

	private void populateQueryParameters(Query query, Map<String, Object> parameters) {
	  LOG.debug("populateQueryParameters "+" "+query+" "+parameters);
		for (Entry<String, Object> entry : parameters.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}
	}
}