package com.intellinx.us.ps.implementation.spring.service.drools.common.step;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import com.intellinx.us.ps.implementation.spring.service.common.cache.IApplicationCache;
import com.intellinx.us.ps.implementation.spring.service.drools.stateful.IDroolsFact;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class StepUtil {
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(StepUtil.class);

	private SpelExpressionParser parser = null;

	public StepUtil(SpelExpressionParser parser) {
		this.parser = parser;
	}

	/**
	 * 
	 * @param step
	 */
	public void prepare(HqlStep step) {

		if (step.getQuery() == null) {
			Assert.notNull(step.getQuery());
		}

		if (step.getQuery().getParameters() != null
				&& !step.getQuery().getParameters().isEmpty()) {

			step.getQuery().setParametersExpressions(
					new HashMap<String, Expression>());

			for (String key : step.getQuery().getParameters().keySet()) {

				String expressionString = step.getQuery().getParameters()
						.get(key);
				step.getQuery().getParametersExpressions()
						.put(key, parser.parseExpression(expressionString));

			}

		}

	}

	/**
	 * 
	 * @param step
	 */
	public void prepare(AbstractStep step) {
		if (step.getWhen() != null) {
			step.setWhenExpressionParsed(parser.parseExpression(step.getWhen()));
		}
	}

	/**
	 * 
	 * @param step
	 */
	public void prepare(ExpressionStep step) {
		step.setExpressionsParsed(parser.parseExpression(step.getExpression()));
	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param step
	 * @return
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean createBatchExecutionCommandFromCache(
			com.intellinx.us.ps.implementation.spring.service.drools.stateless.KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message, AbstractStep step)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		// Check cache for the information to be added to the step
		if (step.getApplicationCache() != null) {
			IApplicationCache cache = step.getApplicationCache();
			if (cache.isKeyInCache(message)) {
				addCommands(cache.get(message), commands, step, false);
				return true;
			}
		}

		return false;

	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param step
	 * @return
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean createBatchExecutionCommandFromCache(
			com.intellinx.us.ps.implementation.spring.service.drools.stateful.KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message, AbstractStep step)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		// Check cache for the information to be added to the step
		if (step.getApplicationCache() != null) {
			IApplicationCache cache = step.getApplicationCache();
			if (cache.isKeyInCache(message)) {
				addCommands(cache.get(message), commands, step, true);
				return true;
			}
		}

		return false;

	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param knowledgeSession
	 * @param evaluationContext
	 * @param step
	 * @param entityManagerFactory
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void createBatchExecutionCommand(
			com.intellinx.us.ps.implementation.spring.service.drools.stateless.KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message,
			EvaluationContext evaluationContext, HqlStep step,
			EntityManagerFactory entityManagerFactory)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(entityManagerFactory);

		List<?> objects = service.executeQuery(step.getQuery(), message,
				entityManager, evaluationContext);

		addCommands(objects, commands, step, false);

		if (step.getApplicationCache() != null) {
			IApplicationCache cache = step.getApplicationCache();
			cache.put(objects, message);
		}

	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param knowledgeSession
	 * @param evaluationContext
	 * @param step
	 * @param entityManagerFactory
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void createBatchExecutionCommand(
			com.intellinx.us.ps.implementation.spring.service.drools.stateful.KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message,
			EvaluationContext evaluationContext, HqlStep step,
			EntityManagerFactory entityManagerFactory)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(entityManagerFactory);

		List<?> objects = service.executeQuery(step.getQuery(), message,
				entityManager, evaluationContext);

		addCommands(objects, commands, step, true);

		if (step.getApplicationCache() != null) {
			IApplicationCache cache = step.getApplicationCache();
			cache.put(objects, message);
		}

	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param knowledgeSession
	 * @param context
	 * @param step
	 * @param entityManagerFactory
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void createBatchExecutionCommand(
			com.intellinx.us.ps.implementation.spring.service.drools.stateless.KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message,
			EvaluationContext context, ExpressionStep step,
			EntityManagerFactory entityManagerFactory)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		Object value = step.getExpressionsParsed().getValue(context, message);

		addCommands(value, commands, step, false);

		if (step.getApplicationCache() != null) {
			IApplicationCache cache = step.getApplicationCache();
			cache.put(value, message);
		}

	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param knowledgeSession
	 * @param context
	 * @param step
	 * @param entityManagerFactory
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void createBatchExecutionCommand(
			com.intellinx.us.ps.implementation.spring.service.drools.stateful.KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message,
			EvaluationContext context, ExpressionStep step,
			EntityManagerFactory entityManagerFactory)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		Object value = step.getExpressionsParsed().getValue(context, message);

		addCommands(value, commands, step, true);

		if (step.getApplicationCache() != null) {
			IApplicationCache cache = step.getApplicationCache();
			cache.put(value, message);
		}

	}

	/**
	 * 
	 * @param object
	 * @param commands
	 * @param step
	 */
	private void addCommands(Object object, List<Command<?>> commands,
			AbstractStep step, boolean stateful) {

		if (object != null) {

			switch (step.getTarget()) {
			case FACT:

				if (stateful && step.getType() == Type.UPDATE) {
					if (object instanceof Collection<?>) {
						Iterator<?> iterator = ((Collection<?>) object).iterator();
						while (iterator.hasNext()) {
							Object obj = iterator.next();
							if(IDroolsFact.class.isInstance(obj)){
								((IDroolsFact) obj).setDroolsIdentifier(step
									.getBeanName());
							}
							else{
								LOGGER.error("Stateful UPDATE fact data does not implement IDroolsFact, returning!!!!!");
								return;
							}
						}
					} else {
						if(IDroolsFact.class.isInstance(object)){
						((IDroolsFact) object).setDroolsIdentifier(step
								.getBeanName());
						}
						else{
							LOGGER.error("Stateful UPDATE fact data does not implement IDroolsFact, returning!!!!!");
							return;
						}
					}
				}

				/*if (object instanceof Collection<?>) {
					commands.add(CommandFactory.newInsertElements(
							(Collection<?>) object, step.getBeanName(), true,
							null));
				} else {
					commands.add(CommandFactory.newInsert(object,
							step.getBeanName()));
				}*/
				if (object instanceof Collection<?>) {
					commands.add(CommandFactory.newInsertElements(
							(Collection<?>) object, null, false,
							null));
				} else {
					commands.add(CommandFactory.newInsert(object,
							null, false, null));
				}
				break;
			case GLOBALS:
				commands.add(CommandFactory.newSetGlobal(step.getParameter(),
						object));
				break;
			default:
				break;
			}

		}

	}
}
