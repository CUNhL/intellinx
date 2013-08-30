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
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

import com.intellinx.us.ps.implementation.spring.common.lookup.step.AbstractStep;
import com.intellinx.us.ps.implementation.spring.common.lookup.step.HqlStep;
import com.intellinx.us.ps.implementation.spring.common.lookup.step.LookupStepUtil;

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

	@Autowired
	private ApplicationContext applicationContext;

	private List<AbstractStep> steps;

	protected static final String CORRELATION_VALUE_PREFIX = "###LOOKUP-";

	protected static final String CORRELATION_STEP_NAME_PREFIX = "###STEP-NAME:";

	protected static final String COMMA = ",";

	protected static final String COLON = ":";

	//
	public static final String HEADER_LOOKUP_ENTITY_ADDED = "LOOKUP_ENTITY_ADDED";

	public static final String POUND = "#";

	private String when;

	private Expression whenExpression;

	private String beanName;

	private EvaluationContext evaluationContext;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		ExpressionParser parser = new SpelExpressionParser();
		LookupStepUtil stepUtil = new LookupStepUtil(parser);

		if (when != null) {
			whenExpression = parser.parseExpression(when);
		}

		for (AbstractStep step : steps) {
			if (step instanceof HqlStep) {
				stepUtil.handleStep((HqlStep) step);
			} else {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn("unsupported Step [" + step.getBeanName() + "]");
			}
		}

		// Evaluation Context
		StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
		BeanResolver beanResolver = new BeanFactoryResolver(applicationContext);
		standardEvaluationContext.setBeanResolver(beanResolver);

		evaluationContext = standardEvaluationContext;

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
	private List<AbstractStep> getActiveSteps(Message<?> message,
			boolean isPersistMode) {

		if (!isPersistMode) {
			return steps;
		} else {

			List<AbstractStep> activeSteps = new ArrayList<AbstractStep>();

			try {

				String stepBeanName = ((String) message.getHeaders().get(
						MessageHeaders.CORRELATION_ID));

				stepBeanName = stepBeanName.substring(stepBeanName
						.indexOf(CORRELATION_STEP_NAME_PREFIX));

				stepBeanName = stepBeanName.replaceFirst(
						CORRELATION_STEP_NAME_PREFIX, "");

				stepBeanName = stepBeanName.substring(0,
						stepBeanName.indexOf("-["));

				for (AbstractStep step : steps) {
					activeSteps.add(step);
					if (step.getBeanName().equals(stepBeanName))
						return activeSteps;
				}

			} catch (Exception e) {
				LOGGER.error(
						"Message does not have correlation id, that should not be possible:"
								+ message.toString(), e);
			}

			return activeSteps;
		}
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

		List<AbstractStep> activeSteps = getActiveSteps(message, isPersistMode);

		//
		for (AbstractStep step : activeSteps) {

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Processing Configurations:" + step.getBeanName());

			try {

				message = executeLookup(step, message, isPersistMode);

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
				LOGGER.error(
						"Error during the transformation:" + message.toString(),
						e);
				break;
			}

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch.lap("LookupService", step.getBeanName());

		}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("LookupService", message.getPayload().getClass()
					.getName());

		return message;
	}

	/**
	 * 
	 * @param entityManager
	 * @param step
	 * @param message
	 * @return
	 * @throws Exception
	 */
	private Message<?> prepareLookupSimple(AbstractStep step,
			Message<?> message, boolean persistEntityMode) throws Exception {

		int size = 1;

		// If the object is List find the size
		for (String arrayKey : step.getTargetObjectsOriginalArrayExpressions()
				.keySet()) {

			Expression e = step.getTargetObjectsOriginalArrayExpressions().get(
					arrayKey);

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

			if (step.getTargetObjectsExpressions() != null) {
				for (String mapKey : step.getTargetObjectsExpressions()
						.keySet()) {
					Map<Long, Expression> map = step
							.getTargetObjectsExpressions().get(mapKey);
					expressionTargetObjectExpressions.add(map.get(key));
				}
			}

			Expression expressionWhen = (step.getWhenExpressions() != null) ? step
					.getWhenExpressions().get(key) : null;

			Expression whenNotMetBehaviorExpression = (step
					.getWhenNotMetBehavior().getWhenNotMetExpressions() != null) ? step
					.getWhenNotMetBehavior().getWhenNotMetExpressions()
					.get(key)
					: null;

			Map<String, Expression> parameterExpressions = step
					.getParameterExpressions().get(key);

			message = executeLookup((HqlStep) step, message,
					expressionTargetObjectExpressions, expressionWhen,
					whenNotMetBehaviorExpression, parameterExpressions,
					persistEntityMode, key, evaluationContext);

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
	 * @param step
	 * @param message
	 * @throws Exception
	 */
	private Message<?> prepareLookupMix(AbstractStep step, Message<?> message,
			boolean persistEntityMode) throws Exception {

		// find all target expressions
		List<Expression> expressionTargetObjectExpressions = new ArrayList<Expression>();

		for (String arrayKey : step.getTargetObjectsOriginalArrayExpressions()
				.keySet()) {

			Expression e = step.getTargetObjectsOriginalArrayExpressions().get(
					arrayKey);

			if (e != null && e.getValue(message) instanceof Collection) {

				Collection<?> list = (Collection<?>) e.getValue(message);

				Map<Long, Expression> map = step.getTargetObjectsExpressions()
						.get(arrayKey);

				for (int size = 0; size < list.size(); size++) {
					Expression expression = map.get(new Long(size));
					expressionTargetObjectExpressions.add(expression);
				}

			} else {
				Map<Long, Expression> map = step.getTargetObjectsExpressions()
						.get(arrayKey);
				Expression expression = map.get(0L);
				expressionTargetObjectExpressions.add(expression);
			}

		}

		// Expression without multiplicity
		Expression expressionWhen = (step.getWhenExpressions() != null) ? step
				.getWhenExpressions().get(0L) : null;

		Expression whenNotMetBehaviorExpression = (step.getWhenNotMetBehavior()
				.getWhenNotMetExpressions() != null) ? step
				.getWhenNotMetBehavior().getWhenNotMetExpressions().get(0L)
				: null;

		Map<String, Expression> parameterExpressions = step
				.getParameterExpressions().get(0L);

		return executeLookup((HqlStep) step, message,
				expressionTargetObjectExpressions, expressionWhen,
				whenNotMetBehaviorExpression, parameterExpressions,
				persistEntityMode, 0, evaluationContext);

	}

	/**
	 * 
	 * @param step
	 * @param message
	 * @throws Exception
	 */
	private Message<?> prepareLookupMultiple(AbstractStep step,
			Message<?> message, boolean persistEntityMode) throws Exception {
		return prepareLookupSimple(step, message, persistEntityMode);
	}

	/**
	 * 
	 * @param step
	 * @param message
	 * @throws Exception
	 */
	private Message<?> executeLookup(AbstractStep step, Message<?> message,
			boolean persistEntityMode) throws Exception {

		boolean containsMultiple = false;
		boolean containsSimple = false;

		if (step.getTargetObjects() != null
				&& !step.getTargetObjects().isEmpty()) {
			for (String key : step.getTargetObjects()) {
				if (key.contains(POUND))
					containsMultiple = true;
				else
					containsSimple = true;
			}
		}

		if (!containsMultiple && containsSimple) {
			return prepareLookupSimple(step, message, persistEntityMode);
		} else if (containsMultiple && containsSimple) {
			return prepareLookupMix(step, message, persistEntityMode);
		} else if (containsMultiple && !containsSimple) {
			return prepareLookupMultiple(step, message, persistEntityMode);
		} else {
			return prepareLookupSimple(step, message, persistEntityMode);
		}

	}

	/**
	 * 
	 * @param step
	 * @param message
	 * @throws Exception
	 */
	private Message<?> executeLookup(HqlStep step, Message<?> message,
			List<Expression> expressionTargetObjectExpressions,
			Expression expressionWhen, Expression whenNotMetBehaviorExpression,
			Map<String, Expression> parameterExpressions,
			boolean persistEntityMode, long index,
			EvaluationContext evaluationContext) throws Exception {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Execute Lookup: persistEntityMode="
					+ persistEntityMode);
		}

		StringBuffer buffer = new StringBuffer(CORRELATION_VALUE_PREFIX);
		buffer.append(step.getBeanName());

		// Check When
		if (LOGGER.isDebugEnabled() && expressionWhen != null) {
			LOGGER.debug("Before Expression When Calculation:"
					+ expressionWhen.getExpressionString() + " == "
					+ expressionWhen.getValue(message, Boolean.class));
		}

		// Check if the message should be evaluated
		if (expressionWhen != null
				&& !expressionWhen.getValue(message, Boolean.class)) {

			if (step.getWhenNotMetBehavior() != null
					&& step.getWhenNotMetBehavior().getType() != null) {

				switch (step.getWhenNotMetBehavior().getType()) {
				case ERROR:

					if (LOGGER.isErrorEnabled())
						LOGGER.error("the configuration [" + getBeanName()
								+ "] had the WHEN expression ["
								+ expressionWhen.getExpressionString()
								+ "] resulting in FALSE for the message:"
								+ message);

					if (whenNotMetBehaviorExpression != null)
						for (Expression expression : expressionTargetObjectExpressions)
							expression.setValue(message,
									whenNotMetBehaviorExpression
											.getValue(message));

					break;

				case WARN:

					if (LOGGER.isWarnEnabled())
						LOGGER.warn("the configuration [" + getBeanName()
								+ "] had the WHEN expression ["
								+ expressionWhen.getExpressionString()
								+ "] resulting in FALSE for the message:"
								+ message);

					if (whenNotMetBehaviorExpression != null)
						for (Expression expression : expressionTargetObjectExpressions)
							expression.setValue(message,
									whenNotMetBehaviorExpression
											.getValue(message));

					break;

				case IGNORE:

					break;

				default:

				}

			} else {

				if (LOGGER.isWarnEnabled())
					LOGGER.debug("the configuration [" + getBeanName() + "/"
							+ step.getBeanName()
							+ "] had the WHEN expression ["
							+ expressionWhen.getExpressionString()
							+ "] resulting in FALSE for the message:"
							+ message.toString());

			}

			return message;
		}

		//
		// Check When
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Before Query Calculation: hql: " + step.getHql());

		// //////
		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(getEntityManagerFactory());

		Query query = entityManager.createQuery(step.getHql());

		for (String key : parameterExpressions.keySet()) {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Setting Parameter [" + key + "]:"
						+ parameterExpressions.get(key).getValue(message));
			}

			Object value = parameterExpressions.get(key).getValue(message);

			if (value == null) {

				LOGGER.error("NULL parameter value passed to HQL ("
						+ step.getHql() + "), for key (" + key + ") message:"
						+ message.toString());

			} else if (value instanceof String
					&& StringUtils.isBlank((String) value)) {

				LOGGER.error("Blank String parameter value passed to HQL ("
						+ step.getHql() + "), for key (" + key + ") message:"
						+ message.toString());

			}

			query.setParameter(key, value);
			buffer.append(COMMA).append(key).append(COLON)
					.append(value.toString());
		}

		if (step.isCacheable()) {
			query.setHint(QueryHints.HINT_CACHEABLE, true);
			if (!StringUtils.isEmpty(step.getCacheRegion())) {
				query.setHint(QueryHints.HINT_CACHE_REGION,
						step.getCacheRegion());
			}
		}

		query.setHint(QueryHints.HINT_READONLY, step.isReadOnly());

		query.setMaxResults(1);

		List<?> result = query.getResultList();

		// The requested object was found
		if (!result.isEmpty()) {

			if (persistEntityMode
					&& (message.getHeaders().get(MessageHeaders.CORRELATION_ID)
							.toString().endsWith(step.getBeanName() + "-["
							+ index + "]"))) {

				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("the entity that was supposed to be added was found send the message back to the MT");
				}

				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(HEADER_LOOKUP_ENTITY_ADDED, "true");
				builder.copyHeaders(map);
				return builder.build();

			}

			Object entity = result.get(0);

			// MERGE !!!
			if (step.getMerge() != null) {
				Object value = step.getMerge().getCalculatedMergeFrom()
						.getValue(evaluationContext, message);
				LookupStepUtil.merge(value, entity, step.getMerge());
			}

			for (Expression expression : expressionTargetObjectExpressions) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Result Found: setting to:"
							+ expression.getExpressionString());
				expression.setValue(message, entity);
			}

			// The requested object was NOT found, the persist is required
		} else if (step.isPersistIfNotFound()) {

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
				builder.removeHeader(MessageHeaders.CORRELATION_ID);
				builder.copyHeaders(map);
				Message<?> newMessage = builder.build();
				return newMessage;

			} else {

				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Due new object found during lookup ["
							+ step.getBeanName()
							+ "], sending message to another thread, adding header with beanName: "
							+ getBeanName());

				buffer.append(CORRELATION_STEP_NAME_PREFIX);
				buffer.append(step.getBeanName()).append("-[").append(index)
						.append("]");

				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				builder.setCorrelationId(buffer.toString());
				return builder.build();

			}

		} else {
			LOGGER.warn("New entity identified [" + step.getBeanName()
					+ "] however it will not be persisted due configuration");
		}

		return message;

	}

	public List<AbstractStep> getSteps() {
		return steps;
	}

	public void setSteps(List<AbstractStep> steps) {
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

}
