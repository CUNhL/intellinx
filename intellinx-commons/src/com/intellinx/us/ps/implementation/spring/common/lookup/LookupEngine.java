package com.intellinx.us.ps.implementation.spring.common.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

import com.intellinx.us.ps.implementation.spring.common.lookup.step.HqlStep;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class LookupEngine implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(LookupEngine.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private EntityManagerFactory entityManagerFactory;

	private List<HqlStep> configurations;

	private static final String QUERY_HINT_READ_ONLY = "org.hibernate.readOnly";

	private static final String QUERY_HINT_CACHEABLE = "org.hibernate.cacheable";

	private static final String QUERY_HINT_CACHE_REGION = "org.hibernate.cacheRegion";

	private static final String POUND = "#";

	private String when;

	private Expression whenExpression;

	private String beanName;

	private String referenceName;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		ExpressionParser parser = new SpelExpressionParser();

		if (when != null)
			whenExpression = parser.parseExpression(when);

		for (HqlStep configuration : configurations)
			handleConfiguration(configuration, parser);

		referenceName = this.getClass().getName() + "-" + getBeanName();

	}

	/**
	 * 
	 * @param targetobject
	 * @return
	 */
	private String getTargetObjectOriginalArray(String targetObject) {
		return targetObject.substring(0, targetObject.indexOf(POUND) - 1);
	}

	/**
	 * 
	 * @param configuration
	 */
	private void handleConfiguration(HqlStep configuration,
			ExpressionParser parser) {

		Expression expression = null;

		// --------------------
		// Initialize Objects
		configuration
				.setTargetObjectsExpressions(new HashMap<String, Map<Long, Expression>>());
		configuration
				.setTargetObjectsOriginalArrayExpressions(new HashMap<String, Expression>());

		// Check target Objects, and add to the List of TargetObjects
		if (configuration.getTargetObjects() == null) {
			configuration.setTargetObjects(new ArrayList<String>());
		}

		if (configuration.getTargetObject() != null) {
			configuration.getTargetObjects().add(
					configuration.getTargetObject());
		}

		for (String targetObject : configuration.getTargetObjects()) {

			Map<Long, Expression> map = new HashMap<Long, Expression>();

			if (!targetObject.contains(POUND)) {

				expression = parser.parseExpression(targetObject);
				map.put(0L, expression);

				configuration.getTargetObjectsOriginalArrayExpressions().put(
						targetObject, null);

			} else {

				String targetObjectsOriginalArray = getTargetObjectOriginalArray(targetObject);

				expression = parser.parseExpression(targetObjectsOriginalArray);

				configuration.getTargetObjectsOriginalArrayExpressions().put(
						targetObject, expression);

				for (long j = 0; j < 1000; j++) {

					expression = parser.parseExpression(targetObject.replace(
							POUND, String.valueOf(j)));
					map.put(j, expression);

				}

			}

			configuration.getTargetObjectsExpressions().put(targetObject, map);

		}

		// --------------------
		if (configuration.getWhen() != null) {
			configuration.setWhenExpressions(new HashMap<Long, Expression>());
			for (long j = 0; j < 1000; j++) {
				if (!configuration.getWhen().contains(POUND)) {
					expression = parser
							.parseExpression(configuration.getWhen());
					configuration.getWhenExpressions().put(j, expression);
					break;
				} else {
					expression = parser.parseExpression(configuration.getWhen()
							.replace(POUND, String.valueOf(j)));
					configuration.getWhenExpressions().put(j, expression);
				}
			}
		}

		// --------------------
		if (configuration.getWhenNotMetBehavior() != null) {
			configuration.getWhenNotMetBehavior().setWhenNotMetExpressions(
					new HashMap<Long, Expression>());
			for (long j = 0; j < 1000; j++) {
				if (!configuration.getWhenNotMetBehavior().getExpression()
						.contains(POUND)) {
					expression = parser.parseExpression(configuration
							.getWhenNotMetBehavior().getExpression());
					configuration.getWhenNotMetBehavior()
							.getWhenNotMetExpressions().put(j, expression);
					break;
				} else {
					expression = parser.parseExpression(configuration
							.getWhenNotMetBehavior().getExpression()
							.replace(POUND, String.valueOf(j)));
					configuration.getWhenNotMetBehavior()
							.getWhenNotMetExpressions().put(j, expression);
				}
			}
		}

		// --------------------
		configuration
				.setParameterExpressions(new HashMap<Long, Map<String, Expression>>());
		for (long j = 0; j < 1000; j++) {
			Map<String, Expression> expressions = new HashMap<String, Expression>();
			for (String key : configuration.getParameters().keySet()) {
				String sp = configuration.getParameters().get(key)
						.replace(POUND, String.valueOf(j));
				expressions.put(key, parser.parseExpression(sp));
			}
			configuration.getParameterExpressions().put(j, expressions);
		}

		// --------------------

	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Object transform(Object object) {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("LookupService", object.getClass()
					.getName(), LOGGER_PERFORMANCE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Start Process for a message type:"
					+ object.getClass().getName());
		}

		// verify when, if false execute the WhenBehavior
		if (whenExpression != null
				&& !whenExpression.getValue(object, Boolean.class)) {
			return object;
		}

		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(getEntityManagerFactory());

		for (HqlStep configuration : getConfigurations()) {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Processing Configurations:"
						+ configuration.getBeanName());
			}

			if (object instanceof Message<?>) {

				Message<?> message = (Message<?>) object;

				if (!message.getHeaders().containsKey(referenceName)) {

					MessageBuilder<?> builder = MessageBuilder
							.fromMessage(message);
					builder = builder.copyHeadersIfAbsent(message.getHeaders());
					builder = builder.setHeader(referenceName,
							new ArrayList<String>());
					message = builder.build();

				}

				List<String> configurationList = (List<String>) message
						.getHeaders().get(referenceName);

				configurationList.add(configuration.getBeanName());
			}

			try {
				executeLookup(entityManager, configuration, object);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch.lap("LookupService", configuration.getBeanName());

		}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("LookupService", object.getClass().getName());

		return object;
	}

	/**
	 * 
	 * @param configuration
	 * @param message
	 * @throws Exception
	 */
	private void executeLookup(EntityManager entityManager,
			HqlStep configuration, Object object) throws Exception {

		long size = 1;

		// If the object is List find the size
		for (String arrayKey : configuration
				.getTargetObjectsOriginalArrayExpressions().keySet()) {

			Expression e = configuration
					.getTargetObjectsOriginalArrayExpressions().get(arrayKey);

			if (e != null && e.getValue(object) instanceof Collection) {
				Collection<?> l = (Collection<?>) e.getValue(object);
				size = l.size();
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Real size of the Array to be processed ["
							+ size + "]");
			}

			break;

		}

		// loop on each item of the array or only 1 if not array (collection)
		for (long key = 0; key < size; key++) {
			//

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Processing [" + key + " / " + size + "]");

			List<Expression> expressionTargetObjectExpressions = new ArrayList<Expression>();

			if (configuration.getTargetObjectsExpressions() != null) {
				for (String mapKey : configuration
						.getTargetObjectsExpressions().keySet()) {
					Map<Long, Expression> map = configuration
							.getTargetObjectsExpressions().get(mapKey);
					expressionTargetObjectExpressions.add(map.get(key));
				}
			}

			Expression expressionWhen = (configuration.getWhenExpressions() != null) ? configuration
					.getWhenExpressions().get(key) : null;
			Expression whenNotMetBehaviorExpression = (configuration
					.getWhenNotMetBehavior().getWhenNotMetExpressions() != null) ? configuration
					.getWhenNotMetBehavior().getWhenNotMetExpressions()
					.get(key)
					: null;
			Map<String, Expression> parameterExpressions = configuration
					.getParameterExpressions().get(key);

			executeLookup(entityManager, configuration, object,
					expressionTargetObjectExpressions, expressionWhen,
					whenNotMetBehaviorExpression, parameterExpressions);

		}

	}

	/**
	 * 
	 * @param configuration
	 * @param message
	 * @throws Exception
	 */
	private void executeLookup(EntityManager entityManager,
			HqlStep configuration, Object object,
			List<Expression> expressionTargetObjectExpressions,
			Expression expressionWhen, Expression whenNotMetBehaviorExpression,
			Map<String, Expression> parameterExpressions) throws Exception {

		// Check When
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Before Expression When Calculation:"
					+ expressionWhen.getExpressionString() + " == "
					+ expressionWhen.getValue(object, Boolean.class));
		}

		if (!expressionWhen.getValue(object, Boolean.class)) {
			if (whenNotMetBehaviorExpression == null)
				return;
			else {
				for (Expression expression : expressionTargetObjectExpressions) {
					expression.setValue(object,
							whenNotMetBehaviorExpression.getValue(object));
				}

			}
		}

		//
		// Check When
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Before Query Calculation: hql: "
					+ configuration.getHql());
		}
		Query query = entityManager.createQuery(configuration.getHql());

		for (String key : parameterExpressions.keySet()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Setting Parameter [" + key + "]:"
						+ parameterExpressions.get(key).getValue(object));
			}
			query.setParameter(key,
					parameterExpressions.get(key).getValue(object));
		}

		if (configuration.isCacheable()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Setting Cacheble");
			}
			query.setHint(QUERY_HINT_CACHEABLE, true);
		}

		if (configuration.isReadOnly()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Setting ReadOnly");
			}
			query.setHint(QUERY_HINT_READ_ONLY, true);
		}

		if (!StringUtils.isEmpty(configuration.getCacheRegion())) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Setting Cache Region");
			}
			query.setHint(QUERY_HINT_CACHE_REGION,
					configuration.getCacheRegion());
		}

		query.setMaxResults(1);

		List<?> result = query.getResultList();

		Object alreadyPersisted = null;

		if (!result.isEmpty()) {

			for (Expression expression : expressionTargetObjectExpressions) {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.trace("Result Found: setting to:"
							+ expression.getExpressionString());
				}

				expression.setValue(object, result.get(0));

			}

		} else if (configuration.isPersistIfNotFound()) {

			// Find one value that is not null;
			// TODO - this would be better if the system configuration could
			// indicate which field is the most appropriated to the save.
			for (Expression expression : expressionTargetObjectExpressions) {
				Object o = expression.getValue(object);
				if (o != null) {
					if (!entityManager.contains(o)) {
						alreadyPersisted = entityManager.merge(o);
						break;
					}
				}
			}

			// Set the values on all fields
			for (Expression expression : expressionTargetObjectExpressions) {
				expression.setValue(object, alreadyPersisted);
			}

		}

	}

	public List<HqlStep> getConfigurations() {
		return configurations;
	}

	public void setConfigurations(List<HqlStep> configurations) {
		this.configurations = configurations;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

}
