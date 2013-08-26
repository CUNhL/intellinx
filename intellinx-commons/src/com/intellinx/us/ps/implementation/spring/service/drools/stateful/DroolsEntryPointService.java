package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.drools.audit.WorkingMemoryFileLogger;
import org.drools.command.BatchExecutionCommand;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.command.runtime.rule.FireAllRulesCommand;
import org.drools.command.runtime.rule.RetractCommand;
import org.drools.runtime.ClassObjectFilter;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import com.intellinx.bom.entity.BasicIncident;
import com.intellinx.us.ps.implementation.spring.service.drools.common.AbstractDroolsService;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.AbstractStep;

/**
 * 
 * @author RenatoM
 * 
 */
public class DroolsEntryPointService extends AbstractDroolsService implements
		BeanNameAware, Transformer, InitializingBean {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DroolsEntryPointService.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private DroolsSessionService droolsSessionService;

	private String entryPointName;

	private boolean multithread;

	private AbstractStep configuration;

	//
	private Map<Class<?>, ObjectFilter> incidentObjectFilters;

	private WorkingMemoryFileLogger fileLogger;

	private static final String INCIDENTS = "INCIDENTS";

	private long count;

	private boolean disable;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		// SpelExpressionParser parser = new SpelExpressionParser();

		/*
		 * if (getConfiguration().getBeforeRuleExecutionActions() != null) {
		 * 
		 * for (LoadDataQuery query : getConfiguration()
		 * .getBeforeRuleExecutionActions()) {
		 * 
		 * if (query.getParameters() != null &&
		 * !query.getParameters().isEmpty()) {
		 * query.setParametersExpressions(new HashMap<String, Expression>());
		 * for (String key : query.getParameters().keySet()) { String
		 * expressionString = query.getParameters() .get(key);
		 * query.getParametersExpressions().put(key,
		 * parser.parseExpression(expressionString)); } } } }
		 */

		// get entry point
		if (entryPointName != null) {
			WorkingMemoryEntryPoint entryPoint = droolsSessionService
					.getKnowledgeSession().getWorkingMemoryEntryPoint(
							entryPointName);
			Assert.notNull(entryPoint,
					"Entrypoint name must exist on the DRL files");
		}

		// Creating Incident Object Filer
		incidentObjectFilters = new HashMap<Class<?>, ObjectFilter>();
		incidentObjectFilters.put(BasicIncident.class, new ClassObjectFilter(
				BasicIncident.class));

		/*
		 * // create EL Expression for input data if
		 * (configuration.getExpressions() != null) {
		 * configuration.setExpressionsParsed(new ArrayList<Expression>()); for
		 * (String expression : configuration.getExpressions()) {
		 * configuration.getExpressionsParsed().add(
		 * parser.parseExpression(expression)); }
		 * 
		 * }
		 */

	}

	/**
	 * 
	 */
	@Override
	public Message<?> transform(Message<?> message) {

		StopWatch stopWatch = null;

		try {

			if (isDisable())
				return message;

			count++;

			if (LOGGER_PERFORMANCE.isDebugEnabled()) {
				stopWatch = new Slf4JStopWatch("DroolsEntryPointService",
						"start", LOGGER_PERFORMANCE);
			}

			StatefulKnowledgeSession knowledgeSession = droolsSessionService
					.getKnowledgeSession();

			EntityManager entityManager = EntityManagerFactoryUtils
					.getTransactionalEntityManager(getEntityManagerFactory());

			WorkingMemoryEntryPoint entryPoint = null;
			if (entryPointName != null) {
				entryPoint = knowledgeSession
						.getWorkingMemoryEntryPoint(entryPointName);
			}

			Object synchronizer = (entryPoint != null) ? entryPoint
					: droolsSessionService;

			BatchExecutionCommand command = createBatchExecutionCommand(
					entityManager, stopWatch, message, entryPoint,
					knowledgeSession);

			synchronized (synchronizer) {

				ExecutionResults results = knowledgeSession.execute(command);

				//
				if (LOGGER_PERFORMANCE.isDebugEnabled()) {
					stopWatch
							.lap("DroolsEntryPointService",
									"after fireAllRules from kb: ["
											+ droolsSessionService
													.getBeanName()
											+ "] - Number of Object in Memory (Fact Count):"
											+ knowledgeSession.getFactCount());
				}

				// Send Incidents To Header
				message = sendObjectsToHeader(message, stopWatch,
						BasicIncident.class, INCIDENTS, true, knowledgeSession);

				/*
				 * // Send Other Objects to the Header if
				 * (configuration.getAfterRuleExecutionActions() != null) { for
				 * (AfterRuleExecutionAction action : configuration
				 * .getAfterRuleExecutionActions()) { message =
				 * sendObjectsToHeader(message, stopWatch,
				 * action.getObjectClass(), action.getHeaderName(),
				 * action.isRetract(), knowledgeSession); } }
				 */
				recycleSession(results, knowledgeSession);
			}

		} catch (Exception e) {
			LOGGER.error("Error when executing queries:" + e.getMessage());
			e.printStackTrace();
		}

		if (LOGGER_PERFORMANCE.isDebugEnabled()) {
			stopWatch.stop("DroolsEntryPointService",
					"DroolsEntryPointService finalized");
		}

		return message;

	}

	/**
	 * 
	 * @return
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	private BatchExecutionCommand createBatchExecutionCommand(
			EntityManager entityManager, StopWatch stopWatch,
			Message<?> message, WorkingMemoryEntryPoint entryPoint,
			StatefulKnowledgeSession knowledgeSession)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		List<Command<?>> commands = new ArrayList<Command<?>>();

		// Add pre execution information from queries
		/*
		 * List<Object> objects = executeQueries(
		 * configuration.getBeforeRuleExecutionActions(), message,
		 * entityManager); if (objects != null) { insert(commands, entryPoint,
		 * PRE_RULE_EXECUTION_ACTION, objects); } if
		 * (LOGGER_PERFORMANCE.isDebugEnabled()) {
		 * stopWatch.lap("DroolsEntryPointService", "after execute queries"); }
		 */

		/*
		 * if (LOGGER_PERFORMANCE.isDebugEnabled()) {
		 * stopWatch.lap("DroolsEntryPointService",
		 * "after creating command to insert queries objects into memory, size:"
		 * + ((objects != null) ? objects.size() : 0)); }
		 */

		/*
		 * // Insert the Payload into the session. List<Object> objectsPayload =
		 * new ArrayList<Object>(); if
		 * (getConfiguration().getExpressionsParsed() != null) { for (Expression
		 * expression : getConfiguration() .getExpressionsParsed()) { Object
		 * object = expression.getValue(message); if (object instanceof
		 * Collection) { Collection<?> collection = (Collection<?>) object; for
		 * (Object innerObject : collection) objectsPayload.add(innerObject); }
		 * else { objectsPayload.add(object); } } } else {
		 * objectsPayload.add(message.getPayload()); }
		 */
		// insert(commands, entryPoint, PAYLOAD, objectsPayload);

		if (LOGGER_PERFORMANCE.isDebugEnabled()) {
			stopWatch.lap("DroolsEntryPointService",
					"after creating command to insert payload into memory");
		}

		/*
		 * if (LOGGER_PERFORMANCE.isDebugEnabled()) { stopWatch
		 * .lap("DroolsEntryPointService", "Number of facts in Payload: size=" +
		 * objectsPayload.size()); }
		 */

		/*
		 * // Insert the head objects into the session. if
		 * (configuration.getHeadFieldToInject() != null) { for (String head :
		 * configuration.getHeadFieldToInject()) { List<Object> objectsHead =
		 * new ArrayList<Object>(); if (message.getHeaders().containsKey(head))
		 * { Object object = message.getHeaders().get(head); if (object
		 * instanceof Collection) { Collection<?> collection = (Collection<?>)
		 * object; for (Object innerObject : collection)
		 * objectsHead.add(innerObject); } else objectsHead.add(object); }
		 * insert(commands, entryPoint, "HEAD[" + head + "]", objectsHead); if
		 * (LOGGER_PERFORMANCE.isDebugEnabled()) {
		 * stopWatch.lap("DroolsEntryPointService",
		 * "after creating command to insert head [" + head +
		 * "] object memory"); } } }
		 */

		//

		// Create the FireAll Command
		commands.add(new FireAllRulesCommand());
		//
		return CommandFactory.newBatchExecution(commands);

	}

	/**
	 * 
	 * @param results
	 */
	private void recycleSession(ExecutionResults results,
			StatefulKnowledgeSession knowledgeSession) {

		List<Command<?>> cmds = new ArrayList<Command<?>>();

		/*
		 * // Retract Header Facts if (configuration.getHeadFieldToInject() !=
		 * null && !configuration.getHeadFieldToInject().isEmpty()) { for
		 * (String head : configuration.getHeadFieldToInject()) { RetractCommand
		 * retractCommand = new RetractCommand( (FactHandle) results
		 * .getFactHandle("HEAD[" + head + "]")); cmds.add(retractCommand); } }
		 */

		// Retract PRE_RULE_EXECUTION_ACTION
		@SuppressWarnings("unchecked")
		List<FactHandle> factHandles = (List<FactHandle>) results
				.getFactHandle("PRE_RULE_EXECUTION_ACTION");
		if (factHandles != null && !factHandles.isEmpty()) {
			for (FactHandle factHandle : factHandles) {
				cmds.add(new RetractCommand(factHandle));
			}
		}
		knowledgeSession.execute(CommandFactory.newBatchExecution(cmds));

	}

	/**
	 * 
	 * 
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * 
	 */
	@SuppressWarnings("unchecked")
	private Message<?> sendObjectsToHeader(Message<?> message,
			StopWatch stopWatch, Class<?> objectClass, String headerName,
			boolean retract, StatefulKnowledgeSession knowledgeSession)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		List<Object> objects = reclaimUsingObjectFilter(knowledgeSession,
				stopWatch, objectClass, retract);

		if (!objects.isEmpty()) {

			List<Object> messageHeader = (List<Object>) message.getHeaders()
					.get(headerName);

			if (messageHeader == null) {
				MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
				Map<String, Object> header = new HashMap<String, Object>();
				header.put(headerName, objects);
				builder.copyHeaders(header);
				message = builder.build();

			} else {
				messageHeader.addAll(objects);
			}

			if (LOGGER_PERFORMANCE.isDebugEnabled()) {
				stopWatch.lap("DroolsEntryPointService",
						"after adding Incidents to the Header");
			}
		}

		return message;

	}

	/**
	 * 
	 * @param cmds
	 * @param entryPoint
	 * @param what
	 * @param objects
	 */
	@SuppressWarnings({ "rawtypes", "unused" })
	private void insert(List<Command<?>> cmds,
			WorkingMemoryEntryPoint entryPoint, String what,
			List<Object> objects) {
		if (objects != null) {
			Command insertElementsCommand = CommandFactory.newInsertElements(
					objects, what, true,
					entryPoint == null ? null : entryPoint.getEntryPointId());
			cmds.add(insertElementsCommand);
		}
	}

	/**
	 * 
	 * @param session
	 * @return
	 */
	private List<Object> reclaimUsingObjectFilter(
			StatefulKnowledgeSession session, StopWatch stopWatch,
			Class<?> objectClass, boolean retract) {

		List<Object> incidents = new ArrayList<Object>();

		//

		ObjectFilter objectFilter = incidentObjectFilters.get(objectClass);

		if (objectFilter == null) {
			objectFilter = new ClassObjectFilter(objectClass);
			incidentObjectFilters.put(objectClass, objectFilter);
		}

		Collection<FactHandle> factHandlers = session
				.getFactHandles(objectFilter);

		if (LOGGER_PERFORMANCE.isDebugEnabled()) {
			stopWatch.lap(
					"DroolsEntryPointService",
					"after GetObject on Incidents ["
							+ objectClass.getSimpleName() + "], size:"
							+ factHandlers.size());
		}

		if (factHandlers.size() > 0) {

			for (FactHandle factHandle : factHandlers) {
				incidents.add(session.getObject(factHandle));
				if (retract) {
					session.retract(factHandle);
				}
			}

			if (LOGGER_PERFORMANCE.isDebugEnabled()) {
				stopWatch.lap("DroolsEntryPointService",
						"after retracting objects");
			}
		}

		return incidents;

	}

	public DroolsSessionService getDroolsSessionService() {
		return droolsSessionService;
	}

	public void setDroolsSessionService(
			DroolsSessionService droolsSessionService) {
		this.droolsSessionService = droolsSessionService;
	}

	public AbstractStep getConfiguration() {
		return configuration;
	}

	public void setConfiguration(AbstractStep configuration) {
		this.configuration = configuration;
	}

	public String getEntryPointName() {
		return entryPointName;
	}

	public void setEntryPointName(String entryPointName) {
		this.entryPointName = entryPointName;
	}

	public WorkingMemoryFileLogger getFileLogger() {
		return fileLogger;
	}

	public void setFileLogger(WorkingMemoryFileLogger fileLogger) {
		this.fileLogger = fileLogger;
	}

	public long getCount() {
		return count;
	}

	public boolean isDisable() {
		return disable;
	}

	public void setDisable(boolean disable) {
		this.disable = disable;
	}

	public boolean isMultithread() {
		return multithread;
	}

	public void setMultithread(boolean multithread) {
		this.multithread = multithread;
	}

}
