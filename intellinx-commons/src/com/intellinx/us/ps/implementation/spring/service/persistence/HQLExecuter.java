package com.intellinx.us.ps.implementation.spring.service.persistence;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

/**
 * 
 * @author RenatoM
 * 
 */
public class HQLExecuter implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(HQLExecuter.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private EntityManagerFactory entityManagerFactory;

	private String hql;

	private Map<String, String> parameters;

	private Map<String, Expression> expressions;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(parameters, "The field [parameters] is obligatory");

		ExpressionParser parser = new SpelExpressionParser();

		expressions = new HashMap<String, Expression>(parameters.size());

		for (String key : parameters.keySet()) {
			Expression exp = parser.parseExpression(parameters.get(key));
			expressions.put(key, exp);
		}

	}

	/**
	 * 
	 * @param paramMessage
	 * @return
	 */
	// @Profiled(tag = "HQLExecuter", normalAndSlowSuffixesEnabled = true,
	// logFailuresSeparately = true, timeThreshold = 5)
	public Message<?> transform(Message<?> message) {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("HQLExecuter", message.getPayload()
					.getClass().getName(), LOGGER_PERFORMANCE);

		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(getEntityManagerFactory());

		Query query = entityManager.createQuery(hql);

		for (String key : expressions.keySet()) {
			Object value = expressions.get(key).getValue(message);
			query.setParameter(key, value);
		}

		int affectedRows = query.executeUpdate();

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Number of Affected hows for the HQL (" + hql + "): "
					+ affectedRows);

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop();

		return message;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public String getHql() {
		return hql;
	}

	public void setHql(String hql) {
		this.hql = hql;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public Map<String, Expression> getExpressions() {
		return expressions;
	}

	public void setExpressions(Map<String, Expression> expressions) {
		this.expressions = expressions;
	}

}
