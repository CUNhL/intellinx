package com.intellinx.us.ps.implementation.spring.service.drools.common;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.hibernate.ejb.QueryHints;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;

import com.intellinx.us.ps.implementation.spring.service.drools.common.query.LoadDataQuery;

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
			Message<?> message, EntityManager entityManager,
			EvaluationContext evaluationContext) throws SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("AbstractDroolsService"
					+ loadDataQuery.getBeanName(), "executeQuery-start",
					LOGGER_PERFORMANCE);

		Query query = entityManager.createQuery(loadDataQuery.getQuery());

		if (loadDataQuery.getParameters() != null)

			for (String key : loadDataQuery.getParameters().keySet()) {

				Expression expression = loadDataQuery
						.getParametersExpressions().get(key);
				Object value = expression.getValue(evaluationContext, message);
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Setting Query parameter: " + key + ", value:"
							+ value);
				query.setParameter(key, value);
			}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop(
					"AbstractDroolsService-" + loadDataQuery.getBeanName(),
					"query object built");

		if (loadDataQuery.isReadOnly()) {
			query.setHint(QueryHints.HINT_READONLY, true);
		}

		if (loadDataQuery.isCacheable()) {
			query.setHint(QueryHints.HINT_CACHEABLE, true);
			if (loadDataQuery.getCacheRegion() != null)
				query.setHint(QueryHints.HINT_CACHE_REGION,
						loadDataQuery.getCacheRegion());
		}

		if (loadDataQuery.getMaxresults() != null) {
			query.setMaxResults(loadDataQuery.getMaxresults().intValue());
		}

		List<?> objects = query.getResultList();

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop(
					"AbstractDroolsService-" + loadDataQuery.getBeanName(),
					"executeQuery-End ()");

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
	protected List<Object> executeQueries(List<LoadDataQuery> queries,
			Message<?> message, EntityManager entityManager,
			StandardEvaluationContext standardEvaluationContext)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		List<Object> result = null;

		if (queries != null && !queries.isEmpty()) {
			result = new ArrayList<Object>();
			for (LoadDataQuery loadDataQuery : queries) {
				List<?> objects = executeQuery(loadDataQuery, message,
						entityManager, standardEvaluationContext);
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
