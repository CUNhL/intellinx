package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.drools.KnowledgeBase;
import org.drools.command.BatchExecutionCommand;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import com.intellinx.us.ps.implementation.spring.service.drools.common.AbstractDroolsService;
import com.intellinx.us.ps.implementation.spring.service.drools.common.query.LoadDataQuery;

/**
 * 
 * @author RenatoM
 * 
 */
public class DroolsSessionService extends AbstractDroolsService implements
		InitializingBean, BeanNameAware {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DroolsSessionService.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private StatefulKnowledgeSession knowledgeSession;

	private KnowledgeBase knowledgeBase;

	private List<LoadDataQuery> queries;

	private Map<LoadDataQuery, Calendar> timeoutMap;

	private EntityManager entityManager;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		// Create the Stateful Session..

		knowledgeSession = knowledgeBase.newStatefulKnowledgeSession();

		// Add the Incident Helper to the memory
		// if (knowledgeSession.getGlobal("incidentHelper") == null) {
		// knowledgeSession.setGlobal("incidentHelper", getIncidentHelper());
		// }

		timeoutMap = new HashMap<LoadDataQuery, Calendar>();

		// get entry point

		if (queries != null)

			for (LoadDataQuery query : queries) {

				Assert.notNull(query.getBeanName(),
						"please provide the query [id]");
				Assert.notNull(query.getQuery(), "please provide the [query]");
				/*
				 * Assert.isTrue( (query.getRefreshRate() != null && query
				 * .getRefreshRateType() != null) || (query.getRefreshRate() ==
				 * null && query .getRefreshRateType() == null),
				 * "please provide [refreshRate] and [refreshRateType] parameters"
				 * );
				 */
				timeoutMap.put(query, Calendar.getInstance());
				loadQueryData(query);

			}

		entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(getEntityManagerFactory());

	}

	/**
	 * 
	 * @param knowledgeSessionWrapper
	 * @param query
	 * @param entryPoint
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void loadQueryData(LoadDataQuery query) throws SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {

		// List<FactHandle> factHandles = new ArrayList<FactHandle>();

		String outIdentifier = "out_identifier_" + query.getBeanName();

		WorkingMemoryEntryPoint entryPoint = null;

		/*
		 * if (query.getEntryPoint() != null) { entryPoint =
		 * knowledgeSession.getWorkingMemoryEntryPoint(query .getEntryPoint());
		 * Assert.notNull(entryPoint, "The entry point [" +
		 * query.getEntryPoint() + "] must be defined on the drl"); }
		 */

		Assert.isNull(
				query.getParameters(),
				"("
						+ getBeanName()
						+ ")Queries loaded during the start of the Rule Session (when=AFTER_LOADING_RULE_ENGINE_SESSION), shall not have parameters");

		List<?> objects = executeQuery(query, null, entityManager, null);

		if (objects != null) {

			List<Command<?>> cmds = new ArrayList<Command<?>>();

			// Retract previous
			/*
			 * if (query.getFactHandles() != null &&
			 * !query.getFactHandles().isEmpty()) { for (FactHandle factHandle :
			 * query.getFactHandles()) { Command<?> retractCommand =
			 * CommandFactory .newRetract(factHandle); cmds.add(retractCommand);
			 * } }
			 */
			Command<?> insertElementsCommand = CommandFactory
					.newInsertElements(
							objects,
							outIdentifier,
							true,
							entryPoint == null ? null : entryPoint
									.getEntryPointId());

			cmds.add(insertElementsCommand);
			BatchExecutionCommand command = CommandFactory.newBatchExecution(
					cmds, "knowledgeSession");
			ExecutionResults results = knowledgeSession.execute(command);

			// Remove all events & facts from the previous load if it is to an
			// entry point

			if (results.getFactHandle(outIdentifier) != null) {

				@SuppressWarnings({ "unchecked", "unused" })
				List<FactHandle> factHandles = (List<FactHandle>) results
						.getFactHandle(outIdentifier);
				/* query.setFactHandles(factHandles); */

			}

		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("(" + getBeanName() + ")Initial Number of Facts: "
					+ knowledgeSession.getFactHandles().size());
			LOGGER.debug("(" + getBeanName() + ")Initial Number of Objects: "
					+ knowledgeSession.getObjects().size());
		}

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
	public StatefulKnowledgeSession getKnowledgeSession()
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		synchronized (this) {

			StopWatch stopWatch = null;

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch = new Slf4JStopWatch("DroolsSessionService",
						"getKnowledgeSession()-Start", LOGGER_PERFORMANCE);

			// Verify preloaded data age
			for (@SuppressWarnings("unused")
			LoadDataQuery query : timeoutMap.keySet()) {

				/*
				 * if (query.getRefreshRate() != null &&
				 * query.getRefreshRateType() != null) {
				 * 
				 * Calendar whenLoaded = timeoutMap.get(query); Calendar
				 * whenTimeOut = Calendar.getInstance();
				 * 
				 * whenTimeOut.add(query.getRefreshRateType().toCalendar(),
				 * -query.getRefreshRate());
				 * 
				 * if (whenLoaded.before(whenTimeOut)) { // Move the date for
				 * the next expiration.. if
				 * (LOGGER_PERFORMANCE.isDebugEnabled()) {
				 * stopWatch.lap("DroolsEntryPointService",
				 * "getKnowledgeSession()-need to refresh session data[" +
				 * query.getBeanName() + "]"); }
				 * whenLoaded.add(query.getRefreshRateType().toCalendar(),
				 * query.getRefreshRate());
				 * 
				 * loadQueryData(query);
				 * 
				 * if (LOGGER_PERFORMANCE.isDebugEnabled()) {
				 * stopWatch.lap("DroolsEntryPointService",
				 * "getKnowledgeSession()-after data loaded [" +
				 * query.getBeanName() + "]"); } }
				 * 
				 * }
				 */
			}

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch.stop("DroolsEntryPointService",
						"getKnowledgeSession()-stop");

		}

		return knowledgeSession;
	}

	public void setKnowledgeSession(StatefulKnowledgeSession knowledgeSession) {
		this.knowledgeSession = knowledgeSession;
	}

	public KnowledgeBase getKnowledgeBase() {
		return knowledgeBase;
	}

	public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
		this.knowledgeBase = knowledgeBase;
	}

	public List<LoadDataQuery> getQueries() {
		return queries;
	}

	public void setQueries(List<LoadDataQuery> queries) {
		this.queries = queries;
	}

	public long getFactCount() throws SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		return getKnowledgeSession().getFactCount();
	}

}
