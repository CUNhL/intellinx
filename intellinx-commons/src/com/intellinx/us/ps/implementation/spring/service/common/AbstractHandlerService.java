package com.intellinx.us.ps.implementation.spring.service.common;

import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;

import com.intellinx.bom.entity.Principal;
import com.intellinx.us.ps.model.common.ICreateInfo;
import com.intellinx.us.ps.model.common.IModifyInfo;

/**
 * 
 * @author RenatoM
 * 
 * @param <P>
 */
public class AbstractHandlerService<P> implements BeanNameAware {

	private static final Logger LOG = LoggerFactory
			.getLogger(AbstractHandlerService.class);

	protected static final String QUERY_PARAMETER = "org.hibernate.readOnly";
	protected static final String QUERY_CACHEABLE = "org.hibernate.cacheable";
	protected static final String QUERY_CACHE_REGION = "org.hibernate.cacheRegion";
	private static final String QUERY_PRICIPAL = "Select p from principal p where p.id=:id";
	private static final String PRINCIPAL = "SYSTEM";
	private static final String USER = "USER";
	private static final String ID = "id";

	private EntityManagerFactory entityManagerFactory;

	private String beanName;

	/**
	 * 
	 * @param query
	 * @param readOnly
	 */
	protected void setReadOnly(Query query, boolean readOnly) {
		query.setHint(QUERY_PARAMETER, readOnly);
	}

	protected void setReadOnly(Query query) {
		setReadOnly(query, true);
	}

	protected void setCacheable(Query query, boolean cacheable) {
		query.setHint(QUERY_CACHEABLE, cacheable);
	}

	protected void setCacheable(Query query) {
		setCacheable(query, true);
	}

	/**
	 * 
	 * @param entityManager
	 * @param p
	 * @param createdBy
	 * @param persist
	 * @return
	 */
	protected P persist(EntityManager entityManager, P p, Principal createdBy,
			boolean persist) {

		if (!(entityManager.contains(p))) {

			if (p instanceof ICreateInfo) {
				setCreated(entityManager, (ICreateInfo) p);
			}

			if (persist) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Persist entity:" + p.getClass().getName() + "//"
							+ p.toString());
				}
				entityManager.persist(p);
			}
		}

		return p;
	}

	protected P merge(EntityManager entityManager, P p) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Persist entity:" + p.getClass().getName() + "//"
					+ p.toString());
		}
		return entityManager.merge(p);

	}

	/**
	 * 
	 * @param entityManager
	 * @param createInfo
	 */
	protected void setCreated(EntityManager entityManager,
			ICreateInfo createInfo) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("setCreated being executed");
		}

		if (createInfo.getCreated() == null) {
			createInfo.setCreated(Calendar.getInstance().getTime());
		}

		Principal principal = findPrincipal(entityManager);
		createInfo.setCreatedBy(principal);
	}

	/**
	 * 
	 * @param entityManager
	 * @param lastModified
	 */
	protected void setLastModified(EntityManager entityManager,
			IModifyInfo lastModified) {

		Principal principal = findPrincipal(entityManager);

		lastModified.setLastModified(Calendar.getInstance().getTime());
		lastModified.setLastModifiedBy(principal);

	}

	/**
	 * 
	 * @param entityManager
	 * @return
	 */
	private Principal findPrincipal(EntityManager entityManager) {

		Principal principal = new Principal(PRINCIPAL);

		principal.setActive(Boolean.valueOf(true));
		principal.setDisplayName(PRINCIPAL);
		principal.setType(USER);

		Query query = entityManager.createQuery(QUERY_PRICIPAL);

		setReadOnly(query);
		setCacheable(query);
		query.setMaxResults(1);
		query.setParameter(ID, PRINCIPAL);

		List<?> principals = query.getResultList();

		if (principals.isEmpty())
			entityManager.persist(principal);
		else {
			principal = (Principal) principals.get(0);
		}

		return principal;
	}

	protected void setCacheable(Query query, String region) {
		query.setHint(QUERY_CACHEABLE, true);
		query.setHint(QUERY_CACHE_REGION, region);
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public String getBeanName() {
		return beanName;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

}
