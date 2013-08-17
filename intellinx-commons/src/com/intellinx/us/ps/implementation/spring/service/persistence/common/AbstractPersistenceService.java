package com.intellinx.us.ps.implementation.spring.service.persistence.common;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.orm.jpa.EntityManagerFactoryUtils;

import com.intellinx.bom.entity.Principal;
import com.intellinx.us.ps.implementation.spring.service.common.AbstractHandlerService;

/**
 * 
 * @author RenatoM
 * 
 * @param <P>
 */
public abstract class AbstractPersistenceService<P> extends
		AbstractHandlerService<P> {
	
	private boolean useCache;
	
	public boolean persist;
	
	private boolean readOnly;
	
	private Integer maxResults;

	/**
	 * 
	 * @param entityManager
	 * @param query
	 * @param p
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected P process(EntityManager entityManager, Query query, P p) {
		setCacheable(query, this.useCache);
		setReadOnly(this.readOnly);
		if (this.maxResults != null)
			query.setMaxResults(this.maxResults.intValue());
		List<?> ps = query.getResultList();
		if (ps.isEmpty()) {
			return persist(entityManager, p, null, this.persist);
		}
		return (P) ps.get(0);
	}

	protected P persist(EntityManager entityManager, P p, Principal createdBy) {
		return super.persist(entityManager, p, createdBy, this.persist);
	}

	protected EntityManager getTransactionalEntityManager() {
		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(getEntityManagerFactory());
		return entityManager;
	}

	public boolean isUseCache() {
		return this.useCache;
	}

	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	public boolean isPersist() {
		return this.persist;
	}

	public void setPersist(boolean persist) {
		this.persist = persist;
	}

	public boolean isReadOnly() {
		return this.readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public Integer getMaxResults() {
		return this.maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		/* 97 */this.maxResults = maxResults;
	}
}
