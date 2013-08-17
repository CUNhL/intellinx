package com.intellinx.us.ps.implementation.spring.job.tasklet;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * 
 * @author RenatoM
 * 
 */
public class HQLExecuterTasklet implements Tasklet, InitializingBean {

	private static final Logger LOG = LoggerFactory
			.getLogger(HQLExecuterTasklet.class);

	private EntityManagerFactory entityManagerFactory;

	private String hql;
	private int maxResults;
	private boolean updateHQL;
	private boolean trunc;
	private Map<String, String> parameters;

	private Map<String, Expression> expressions;

	/**
	 * 
	 */
	public void afterPropertiesSet() throws Exception {

		ExpressionParser parser = new SpelExpressionParser();

		expressions = new HashMap<String, Expression>(parameters.size());

		for (String key : parameters.keySet()) {
			Expression exp = parser.parseExpression(parameters.get(key));
			expressions.put(key, exp);
		}

	}

	/**
	 * 
	 */
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		if (isUpdateHQL())
			executeUpdate();
		else {
			executeQuery();
		}
		return null;
	}

	/**
	 * 
	 */
	private void executeQuery() {
		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(this.entityManagerFactory);

		if (LOG.isInfoEnabled()) {
			LOG.info("Start HQL [" + this.hql + "]");
		}

		Query query = entityManager.createQuery(this.hql);
		if (this.maxResults != 0) {
			query.setMaxResults(this.maxResults);
		}
		List<?> list = query.getResultList();

		for (Iterator<?> localIterator = list.iterator(); localIterator
				.hasNext();) {
			Object object = localIterator.next();
			if (LOG.isInfoEnabled())
				LOG.info("RESULT:" + object.toString());
		}

		if (LOG.isInfoEnabled())
			LOG.info("Execute HQL [" + this.hql + "] Done");

	}

	/**
	 * 
	 */
	private void executeUpdate() {
		//
		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(this.entityManagerFactory);

		Query query = entityManager.createQuery(this.hql);

		for (String key : this.parameters.keySet()) {
			Object value = this.parameters.get(key);
			if ((this.trunc) && (value instanceof Date)) {
				value = DateUtils.truncate((Date) value, 5);
			}
			query.setParameter(key, value);
		}

		query.executeUpdate();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public String getHql() {
		return this.hql;
	}

	public void setHql(String hql) {
		this.hql = hql;
	}

	public int getMaxResults() {
		return this.maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public boolean isUpdateHQL() {
		return this.updateHQL;
	}

	public void setUpdateHQL(boolean updateHQL) {
		this.updateHQL = updateHQL;
	}

	public boolean isTrunc() {
		return this.trunc;
	}

	public void setTrunc(boolean trunc) {
		this.trunc = trunc;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

}
