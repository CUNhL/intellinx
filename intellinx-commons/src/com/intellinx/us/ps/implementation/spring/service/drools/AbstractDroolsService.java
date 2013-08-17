package com.intellinx.us.ps.implementation.spring.service.drools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;

/**
 * 
 * @author RenatoM
 * 
 */
public class AbstractDroolsService implements BeanNameAware {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(AbstractDroolsService.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private static final String QUERY_HINT_READ_ONLY = "org.hibernate.readOnly";

	private static final String QUERY_HINT_CACHEABLE = "org.hibernate.cacheable";

	private static final String QUERY_HINT_CACHE_REGION = "org.hibernate.cacheRegion";

	private String beanName;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/**
	 * 
	 * @param loadDataQuery
	 * @param message
	 * @return
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public List<?> executeQuery(LoadDataQuery loadDataQuery,
			Message<?> message, EntityManager entityManager)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("AbstractDroolsService",
					"executeQuery-start", LOGGER_PERFORMANCE);

		if (!loadDataQuery.isActive())
			return null;

		Query query = entityManager.createQuery(loadDataQuery.getQuery());

		if (loadDataQuery.getParameters() != null)
			for (String key : loadDataQuery.getParameters().keySet()) {
				Expression expression = loadDataQuery
						.getParametersExpressions().get(key);
				Object value = expression.getValue(message);
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Setting Query parameter: " + key + ", value:"
							+ value);
				query.setParameter(key, value);
			}

		if (loadDataQuery.isReadOnly()) {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Query set to QUERY_READ_ONLY");
			query.setHint(QUERY_HINT_READ_ONLY, Boolean.valueOf(true));
		}

		if (loadDataQuery.isCacheable()) {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Query set to QUERY_CACHEABLE for the region ("
						+ loadDataQuery.getCacheRegion() + ")");
			query.setHint(QUERY_HINT_CACHEABLE, Boolean.valueOf(true));
			if (loadDataQuery.getCacheRegion() != null)
				query.setHint(QUERY_HINT_CACHE_REGION,
						loadDataQuery.getCacheRegion());
		}

		List<?> objects = query.getResultList();

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("AbstractDroolsService", "executeQuery-End ("
					+ objects.size() + ")");

		return objects;
	}

	/**
	 * 
	 * @param object
	 * @return
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public List<Object> executeQueries(List<LoadDataQuery> queries,
			Message<?> message, EntityManager entityManager)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		List<Object> result = null;

		if (queries != null && !queries.isEmpty()) {
			result = new ArrayList<Object>();
			for (LoadDataQuery loadDataQuery : queries) {
				List<?> objects = executeQuery(loadDataQuery, message,
						entityManager);
				if (objects != null && !objects.isEmpty())
					result.addAll(objects);
			}
		}

		return result;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

}
