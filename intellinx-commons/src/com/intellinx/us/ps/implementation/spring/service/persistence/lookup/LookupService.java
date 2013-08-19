package com.intellinx.us.ps.implementation.spring.service.persistence.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ejb.QueryHints;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

import com.intellinx.us.ps.implementation.spring.common.lookup.step.HqlStep;

/**
 * 
 * @author RenatoM
 * 
 */
public class LookupService implements InitializingBean, Transformer,
		BeanNameAware {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(LookupService.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private List<HqlStep> steps;

	protected static final String CORRELATION_VALUE_PREFIX = "###LOOKUP-";

	protected static final String CORRELATION_STEP_NAME_PREFIX = "###STEP-NAME:";

	protected static final String COMMA = ",";

	protected static final String COLON = ":";

	//
	public static final String HEADER_LOOKUP_ENTITY_ADDED = "LOOKUP_ENTITY_ADDED";

	protected static final String POUND = "#";

	private String when;

	private Expression whenExpression;

	private String beanName;

	private List<MergeService> mergeServices;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();
		if (when != null)
			whenExpression = parser.parseExpression(when);
		for (HqlStep step : steps)
			handleStep(step, parser);
	}

	/**
	 * 
	 */
	@Override
	@ServiceActivator
	public Message<?> transform(Message<?> message) {
		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isInfoEnabled())
			stopWatch = new Slf4JStopWatch("LookupService", message
					.getPayload().getClass().getName(), LOGGER_PERFORMANCE);

		Message<?> messageProcessed = transform(message, false);

		if (stopWatch != null)
			stopWatch.stop("LookupService", "transform-done");

		return messageProcessed;
	}

	/**
	 * 
	 * @return
	 */
	private List<HqlStep> getConfigurations(Message<?> message,
			boolean isPersistMode) {
		if (!isPersistMode) {
			return steps;
		} else {
			List<HqlStep> list = new ArrayList<HqlStep>();

			try {
				String stepBeanName = ((String) message.getHeaders().get(
						MessageHeaders.CORRELATION_ID));

				stepBeanName = stepBeanName.substring(stepBeanName
						.indexOf(CORRELATION_STEP_NAME_PREFIX));

				stepBeanName = stepBeanName.replaceFirst(
						CORRELATION_STEP_NAME_PREFIX, "");

				stepBeanName = stepBeanName.substring(0,
						stepBeanName.indexOf("-["));

				for (HqlStep step : steps) {
					list.add(step);
					if (step.getBeanName().equals(stepBeanName))
						return list;
				}
			} catch (Exception e) {
				LOGGER.error("Message does not have correlation id, that should not be possible:"
						+ message.toString());
			}

			return list;
		}
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
	 * @param step
	 */
	private void handleStep(HqlStep step, ExpressionParser parser) {

		Expression expression = null;

		// --------------------
		// Initialize Objects
		step.setTargetObjectsExpressions(new HashMap<String, Map<Long, Expression>>());
		step.setTargetObjectsOriginalArrayExpressions(new HashMap<String, Expression>());

		// Check target Objects, and add to the List of TargetObjects
		if (step.getTargetObjects() == null) {
			step.setTargetObjects(new ArrayList<String>());
		}

		if (step.getTargetObject() != null) {
			step.getTargetObjects().add(step.getTargetObject());
		}

		for (String targetObject : step.getTargetObjects()) {

			Map<Long, Expression> map = new HashMap<Long, Expression>();

			if (!targetObject.contains(POUND)) {

				expression = parser.parseExpression(targetObject);
				map.put(0L, expression);

				step.getTargetObjectsOriginalArrayExpressions().put(
						targetObject, null);

			} else {

				String targetObjectsOriginalArray = getTargetObjectOriginalArray(targetObject);

				expression = parser.parseExpression(targetObjectsOriginalArray);

				step.getTargetObjectsOriginalArrayExpressions().put(
						targetObject, expression);

				for (long j = 0; j < 1000; j++) {

					expression = parser.parseExpression(targetObject.replace(
							POUND, String.valueOf(j)));
					map.put(j, expression);

				}

			}

			step.getTargetObjectsExpressions().put(targetObject, map);

		}

		// --------------------
		if (step.getWhen() != null) {
			step.setWhenExpressions(new HashMap<Long, Expression>());
			for (long j = 0; j < 1000; j++) {
				if (!step.getWhen().contains(POUND)) {
					expression = parser.parseExpression(step.getWhen());
					step.getWhenExpressions().put(j, expression);
					break;
				} else {
					expression = parser.parseExpression(step.getWhen().replace(
							POUND, String.valueOf(j)));
					step.getWhenExpressions().put(j, expression);
				}
			}
		}

		// --------------------
		if (step.getWhenNotMetBehavior() != null
				&& StringUtils.isNotBlank(step.getWhenNotMetBehavior()
						.getExpression())) {

			step.getWhenNotMetBehavior().setWhenNotMetExpressions(
					new HashMap<Long, Expression>());
			for (long j = 0; j < 1000; j++) {
				if (!step.getWhenNotMetBehavior().getExpression()
						.contains(POUND)) {
					expression = parser.parseExpression(step
							.getWhenNotMetBehavior().getExpression());
					step.getWhenNotMetBehavior().getWhenNotMetExpressions()
							.put(j, expression);
					break;
				} else {
					expression = parser.parseExpression(step
							.getWhenNotMetBehavior().getExpression()
							.replace(POUND, String.valueOf(j)));
					step.getWhenNotMetBehavior().getWhenNotMetExpressions()
							.put(j, expression);
				}
			}

		}

		// --------------------
		step.setParameterExpressions(new HashMap<Long, Map<String, Expression>>());
		for (long j = 0; j < 1000; j++) {
			Map<String, Expression> expressions = new HashMap<String, Expression>();
			for (String key : step.getParameters().keySet()) {
				String sp = step.getParameters().get(key)
						.replace(POUND, String.valueOf(j));
				expressions.put(key, parser.parseExpression(sp));
			}
			step.getParameterExpressions().put(j, expressions);
		}

		// --------------------

	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	public Message<?> transform(Message<?> message, boolean isPersistMode) {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("LookupService", message
					.getPayload().getClass().getName(), LOGGER_PERFORMANCE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Start Process for a message type:"
					+ message.getPayload().getClass().getName());
		}

		// verify when, if false execute the WhenBehavior
		if (whenExpression != null
				&& !whenExpression.getValue(message, Boolean.class)) {
			return message;
		}

		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(getEntityManagerFactory());

		List<HqlStep> _configurations = getConfigurations(message,
				isPersistMode);
		for (HqlStep configuration : _configurations) {

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Processing Configurations:"
						+ configuration.getBeanName());

			try {

				message = executeLookup(entityManager, configuration, message,
						isPersistMode);

				// check if the message has an item that should be redirected

				if (isPersistMode
						&& message.getHeaders().containsKey(
								HEADER_LOOKUP_ENTITY_ADDED)) {
					// job already done

					if (LOGGER.isDebugEnabled())
						LOGGER.debug("in persist mode and entity was added or found");
					break;
				}

				if (!isPersistMode
						&& message.getHeaders().containsKey(
								MessageHeaders.CORRELATION_ID)) {
					// a new entity was found

					if (LOGGER.isDebugEnabled())
						LOGGER.debug("not in persist mode and a NEW entity was found");

					break;
				}

			} catch (Exception e) {
				e.printStackTrace();
				break;
			}

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch.lap("LookupService", configuration.getBeanName());

		}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("LookupService", message.getPayload().getClass()
					.getName());

		return message;
	}

	/**
	 * 
	 * @param entityManager
	 * @param configuration
	 * @param message
	 * @return
	 * @throws Exception
	 */
	private Message<?> prepareLookupSimple(EntityManager entityManager,
			HqlStep configuration, Message<?> message, boolean persistEntityMode)
			throws Exception {

		int size = 1;

		// If the object is List find the size
		for (String arrayKey : configuration
				.getTargetObjectsOriginalArrayExpressions().keySet()) {

			Expression e = configuration
					.getTargetObjectsOriginalArrayExpressions().get(arrayKey);

			if (e != null && e.getValue(message) instanceof Collection) {
				Collection<?> l = (Collection<?>) e.getValue(message);
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
				LOGGER.debug("Processing [" + (key + 1) + " / " + (size) + "]");

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

			message = executeLookup(entityManager, configuration, message,
					expressionTargetObjectExpressions, expressionWhen,
					whenNotMetBehaviorExpression, parameterExpressions,
					persistEntityMode, key);

			//
			// check if the message has an item that should be redirected
			if (!persistEntityMode
					&& message.getHeaders().containsKey(
							MessageHeaders.CORRELATION_ID)) {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Persist Mode got enabled, breaking the loop.");
				}

				break;

			} else if (persistEntityMode
					&& message.getHeaders().containsKey(
							HEADER_LOOKUP_ENTITY_ADDED)) {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("New Entity Added, breaking the loop for multiple");
				}

				break;
			}

		}

		return message;
	}

	/**
	 * the prepare lookup mix, will find one value and set to multiple targets,
	 * and some of these targets are arrays
	 * 
	 * expression when does not have multiplicity
	 * 
	 * expression "whenNotMetBehaviorExpression" does not have multiplicity
	 * 
	 * expression "parameterExpressions" does not have multiplicity
	 * 
	 * @param configuration
	 * @param message
	 * @throws Exception
	 */
	private Message<?> prepareLookupMix(EntityManager entityManager,
			HqlStep configuration, Message<?> message, boolean persistEntityMode)
			throws Exception {

		// find all target expressions
		List<Expression> expressionTargetObjectExpressions = new ArrayList<Expression>();

		for (String arrayKey : configuration
				.getTargetObjectsOriginalArrayExpressions().keySet()) {

			Expression e = configuration
					.getTargetObjectsOriginalArrayExpressions().get(arrayKey);

			if (e != null && e.getValue(message) instanceof Collection) {

				Collection<?> list = (Collection<?>) e.getValue(message);

				Map<Long, Expression> map = configuration
						.getTargetObjectsExpressions().get(arrayKey);

				for (int size = 0; size < list.size(); size++) {
					Expression expression = map.get(new Long(size));
					expressionTargetObjectExpressions.add(expression);
				}

			} else {
				Map<Long, Expression> map = configuration
						.getTargetObjectsExpressions().get(arrayKey);
				Expression expression = map.get(0L);
				expressionTargetObjectExpressions.add(expression);
			}

		}

		// Expression without multiplicity
		Expression expressionWhen = (configuration.getWhenExpressions() != null) ? configuration
				.getWhenExpressions().get(0L) : null;

		Expression whenNotMetBehaviorExpression = (configuration
				.getWhenNotMetBehavior().getWhenNotMetExpressions() != null) ? configuration
				.getWhenNotMetBehavior().getWhenNotMetExpressions().get(0L)
				: null;

		Map<String, Expression> parameterExpressions = configuration
				.getParameterExpressions().get(0L);

		return executeLookup(entityManager, configuration, message,
				expressionTargetObjectExpressions, expressionWhen,
				whenNotMetBehaviorExpression, parameterExpressions,
				persistEntityMode, 0);

	}

	/**
	 * 
	 * @param configuration
	 * @param message
	 * @throws Exception
	 */
	private Message<?> prepareLookupMultiple(EntityManager entityManager,
			HqlStep configuration, Message<?> message, boolean persistEntityMode)
			throws Exception {
		return prepareLookupSimple(entityManager, configuration, message,
				persistEntityMode);
	}

	/**
	 * 
	 * @param configuration
	 * @param message
	 * @throws Exception
	 */
	private Message<?> executeLookup(EntityManager entityManager,
			HqlStep configuration, Message<?> message, boolean persistEntityMode)
			throws Exception {

		boolean containsMultiple = false;
		boolean containsSimple = false;
		if (configuration.getTargetObjects() != null
				&& !configuration.getTargetObjects().isEmpty())
			for (String key : configuration.getTargetObjects()) {
				if (key.contains(POUND))
					containsMultiple = true;
				else
					containsSimple = true;
			}

		if (!containsMultiple && containsSimple) {
			return prepareLookupSimple(entityManager, configuration, message,
					persistEntityMode);
		} else if (containsMultiple && containsSimple) {
			return prepareLookupMix(entityManager, configuration, message,
					persistEntityMode);
		} else if (containsMultiple && !containsSimple) {
			return prepareLookupMultiple(entityManager, configuration, message,
					persistEntityMode);
		} else {
			return prepareLookupSimple(entityManager, configuration, message,
					persistEntityMode);
		}

	}

	/**
	 * 
	 * @param configuration
	 * @param message
	 * @throws Exception
	 */
	private Message<?> executeLookup(EntityManager entityManager,
			HqlStep configuration, Message<?> message,
			List<Expression> expressionTargetObjectExpressions,
			Expression expressionWhen, Expression whenNotMetBehaviorExpression,
			Map<String, Expression> parameterExpressions,
			boolean persistEntityMode, long index) throws Exception {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Execute Lookup: persistEntityMode="
					+ persistEntityMode);
		}

		StringBuffer buffer = new StringBuffer(CORRELATION_VALUE_PREFIX);
		buffer.append(configuration.getBeanName());

		// Check When
		if (LOGGER.isDebugEnabled() && expressionWhen != null) {
			LOGGER.debug("Before Expression When Calculation:"
					+ expressionWhen.getExpressionString() + " == "
					+ expressionWhen.getValue(message, Boolean.class));
		}

		// Check if the message should be evaluated
		if (expressionWhen != null
				&& !expressionWhen.getValue(message, Boolean.class)) {

			if (whenNotMetBehaviorExpression != null) {

				switch (configuration.getWhenNotMetBehavior().getType()) {
				case WARN:

					if (LOGGER.isWarnEnabled())
						LOGGER.warn("the configuration [" + getBeanName()
								+ "] had the WHEN expression ["
								+ expressionWhen.getExpressionString()
								+ "] resulting in FALSE for the message:"
								+ message);

				case ERROR:

					if (LOGGER.isErrorEnabled())
						LOGGER.error("the configuration [" + getBeanName()
								+ "] had the WHEN expression ["
								+ expressionWhen.getExpressionString()
								+ "] resulting in FALSE for the message:"
								+ message);

				default:
					for (Expression expression : expressionTargetObjectExpressions)
						expression.setValue(message,
								whenNotMetBehaviorExpression.getValue(message));
				}

			} else {

				if (LOGGER.isWarnEnabled())
					LOGGER.debug("the configuration [" + getBeanName()
							+ "] had the WHEN expression ["
							+ expressionWhen.getExpressionString()
							+ "] resulting in FALSE for the message:" + message);

			}

			return message;
		}

		//
		// Check When
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Before Query Calculation: hql: "
					+ configuration.getHql());

		Query query = entityManager.createQuery(configuration.getHql());

		for (String key : parameterExpressions.keySet()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Setting Parameter [" + key + "]:"
						+ parameterExpressions.get(key).getValue(message));
			}
			Object value = parameterExpressions.get(key).getValue(message);

			if (value == null) {

				LOGGER.error("NULL parameter value passed to HQL ("
						+ configuration.getHql() + "), for key (" + key
						+ ") message:" + message.toString());

			} else if (value instanceof String
					&& StringUtils.isBlank((String) value)) {

				LOGGER.error("Blank String parameter value passed to HQL ("
						+ configuration.getHql() + "), for key (" + key
						+ ") message:" + message.toString());

			}

			query.setParameter(key, value);
			buffer.append(COMMA).append(key).append(COLON)
					.append(value.toString());
		}

		if (configuration.isCacheable()) {
			query.setHint(QueryHints.HINT_CACHEABLE, true);
		}

		if (configuration.isReadOnly()) {
			query.setHint(QueryHints.HINT_READONLY, true);
		}

		if (!StringUtils.isEmpty(configuration.getCacheRegion())) {
			query.setHint(QueryHints.HINT_CACHE_REGION,
					configuration.getCacheRegion());
		}

		query.setMaxResults(1);

		List<?> result = query.getResultList();

		// The requested object was found
		if (!result.isEmpty()) {

			if (persistEntityMode
					&& (message.getHeaders().get(MessageHeaders.CORRELATION_ID)
							.toString().endsWith(configuration.getBeanName()
							+ "-[" + index + "]"))) {

				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("the entity that was supposed to be added was found send the message back to the MT");
				}

				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				Map<String, Object> map = new HashMap<String, Object>();
				// builder.pushSequenceDetails(null, 1, 1).popSequenceDetails();
				map.put(HEADER_LOOKUP_ENTITY_ADDED, "true");
				builder.copyHeaders(map);
				Message<?> newMessage = builder.build();
				return newMessage;

			}

			for (Expression expression : expressionTargetObjectExpressions) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Result Found: setting to:"
							+ expression.getExpressionString());
				expression.setValue(message, result.get(0));
			}

			// The requested object was NOT found, the persist is required
		} else if (configuration.isPersistIfNotFound()) {

			if (persistEntityMode) {

				for (Expression expression : expressionTargetObjectExpressions) {
					Object o = expression.getValue(message);

					if (o != null) {

						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("persistEntityMode is true, persisting object ["
									+ o + "]");
						}

						if (!entityManager.contains(o)) {
							entityManager.persist(o);
							break;
						}
					}
				}

				// send the message back to the MT
				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(HEADER_LOOKUP_ENTITY_ADDED, "true");
				// builder.pushSequenceDetails(null, 1, 1).popSequenceDetails();
				builder.removeHeader(MessageHeaders.CORRELATION_ID);
				builder.copyHeaders(map);
				Message<?> newMessage = builder.build();
				return newMessage;

			} else {

				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Due new object found during lookup ["
							+ configuration.getBeanName()
							+ "], sending message to another thread, adding header with beanName: "
							+ getBeanName());

				buffer.append(CORRELATION_STEP_NAME_PREFIX);
				buffer.append(configuration.getBeanName()).append("-[")
						.append(index).append("]");

				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				// builder.pushSequenceDetails(buffer.toString(), 1, 1)
				// .popSequenceDetails();
				builder.setCorrelationId(buffer.toString());
				Message<?> newMessage = builder.build();
				return newMessage;

			}

		} else {
			LOGGER.warn("New entity identified [" + configuration.getBeanName()
					+ "] however it will not be persisted due configuration");
		}

		return message;

	}

	public List<HqlStep> getSteps() {
		return steps;
	}

	public void setSteps(List<HqlStep> steps) {
		this.steps = steps;
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

	public List<MergeService> getMergeServices() {
		return mergeServices;
	}

	public void setMergeServices(List<MergeService> mergeServices) {
		this.mergeServices = mergeServices;
	}

}
