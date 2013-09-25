package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.drools.base.MapGlobalResolver;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.command.runtime.rule.FireAllRulesCommand;
import org.drools.command.runtime.rule.HaltCommand;
import org.drools.command.runtime.rule.RetractCommand;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
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
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.Assert;

import com.intellinx.us.ps.implementation.spring.service.drools.common.AbstractDroolsService;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.AbstractStep;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.ExpressionStep;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.HqlStep;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.StepUtil;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.Target;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.Type;

/**
 * 
 * @author RenatoM
 * 
 */
public class PseudoStatelessKnowledgeSessionService extends
		AbstractDroolsService implements BeanNameAware, Transformer,
		InitializingBean {

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private static final Logger LOGGER = LoggerFactory
			.getLogger(PseudoStatelessKnowledgeSessionService.class);

	@Autowired
	private ApplicationContext applicationContext;

	private PseudoStatelessKnowledgeSessionFactory pseudoStatelessKnowledgeSessionFactory;

	private ObjectPool<StatefulKnowledgeSession> pool;

	private List<AbstractStep> steps;

	private EvaluationContext evaluationContext;

	private String when;

	private Expression calculatedWhenExpression;

	private int poolSize = 0;

	private StepUtil stepUtil;

	private int disposeInterval = 0;

	private long lastDisposed;

	private long startMilli;

	private Map<Integer, Map<String, Integer>> updateMap;

	private boolean useInterface;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(pseudoStatelessKnowledgeSessionFactory);
		Assert.notNull(getBeanName(), "Bean Name is required");
		Assert.notNull(applicationContext);

		stepUtil = new StepUtil(new SpelExpressionParser(), useInterface);

		// Prepare Steps
		if (steps != null)

			for (AbstractStep step : steps) {

				stepUtil.prepare(step);

				Assert.notNull(step.getTarget(), "Target is required");
				if (step.getTarget() == Target.FACT) {
					Assert.notNull(step.getType(),
							"Type is required for steps with target FACT");
				}
				if (step instanceof HqlStep) {
					stepUtil.prepare((HqlStep) step);
				} else if (step instanceof ExpressionStep) {
					stepUtil.prepare((ExpressionStep) step);
				}

			}

		// When
		if (when != null) {
			SpelExpressionParser parser = new SpelExpressionParser();
			calculatedWhenExpression = parser.parseExpression(when);
		}

		// Pool of Drools Sessions
		if (poolSize > 0) {
			pool = new StackObjectPool<StatefulKnowledgeSession>(
					pseudoStatelessKnowledgeSessionFactory, getPoolSize());
		}

		// Evaluation Context
		StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
		BeanResolver beanResolver = new BeanFactoryResolver(applicationContext);
		standardEvaluationContext.setBeanResolver(beanResolver);

		evaluationContext = standardEvaluationContext;

		if (disposeInterval > 0) {
			lastDisposed = new Date().getTime();
		}

		startMilli = new Date().getTime();
		updateMap = new HashMap<Integer, Map<String, Integer>>();

	}

	/**
	 * 
	 */
	@Override
	@ServiceActivator
	public Message<?> transform(Message<?> message) {

		// When for the Bean
		if (calculatedWhenExpression != null
				&& !calculatedWhenExpression.getValue(evaluationContext,
						message, Boolean.class)) {
			return message;
		}

		StopWatch stopWatch = null;
		if (LOGGER_PERFORMANCE.isInfoEnabled())
			stopWatch = new Slf4JStopWatch("KnowledgeSessionService",
					this.getBeanName(), LOGGER_PERFORMANCE);

		StatefulKnowledgeSession knowledgeSession = null;

		try {

			if (poolSize > 0) {
				// Check if time expired and invalidate pool's map
				if (disposeInterval > 0) {
					Date tempDate = new Date();
					if (tempDate.getTime() - lastDisposed > disposeInterval * 60000) {
						pseudoStatelessKnowledgeSessionFactory.invalidatePool();
						lastDisposed = tempDate.getTime();

						updateMap.clear();

						if (LOGGER_PERFORMANCE.isDebugEnabled())
							stopWatch.lap("KnowledgeSessionService",
									"After invalidating pool");
					}
				}
				knowledgeSession = pool.borrowObject();
			} else {
				knowledgeSession = pseudoStatelessKnowledgeSessionFactory
						.getNewStatefulKnowledgeSession();
			}

			List<Command<?>> commands = new ArrayList<Command<?>>();

			boolean isNewSession = (knowledgeSession.getFactCount() == 0 && ((MapGlobalResolver) knowledgeSession
					.getGlobals()).getGlobals().length == 0);

			if (!isNewSession) {

				// retract all but target type update facts from previous
				// runs

				if (LOGGER_PERFORMANCE.isDebugEnabled())
					stopWatch.lap("KnowledgeSessionService",
							"Before retracting");
				ObjectFilter objectFilter;
				if (!useInterface) {
					objectFilter = new CollectionsDifferenceObjectFilter(
							knowledgeSession.getObjects(), steps);
				} else {
					objectFilter = new IdentifierlessObjectFilter();
				}
				Collection<FactHandle> factHandles = knowledgeSession
						.getFactHandles(objectFilter);
				if (factHandles != null && !factHandles.isEmpty()) {
					for (FactHandle factHandle : factHandles) {
						commands.add(new RetractCommand(factHandle));
					}
					if (LOGGER_PERFORMANCE.isDebugEnabled())
						stopWatch.lap("KnowledgeSessionService",
								"After Retract, added " + factHandles.size()
										+ " command(s) to retract facts");
				} else {
					if (LOGGER_PERFORMANCE.isDebugEnabled())
						stopWatch.lap("KnowledgeSessionService",
								"After retract, nothing needs removing");
				}
			}

			Long currentMilli = new Date().getTime();

			for (AbstractStep step : steps) {

				// Set update map if new, check if needs update if not
				boolean update = false;
				if (step.getType() == Type.UPDATE
						&& step.getTarget() == Target.FACT
						&& step.getUpdateInterval() > 0) {
					if (isNewSession) {
						if (updateMap.containsKey(knowledgeSession.getId())) {
							Map<String, Integer> tempMap = updateMap
									.get(knowledgeSession.getId());
							tempMap.put(step.getBeanName(),
									(int) ((currentMilli - startMilli) / (step
											.getUpdateInterval() * 60000)));
						} else {
							Map<String, Integer> tempMap = new HashMap<String, Integer>();
							tempMap.put(step.getBeanName(),
									(int) ((currentMilli - startMilli) / (step
											.getUpdateInterval() * 60000)));
							updateMap.put(knowledgeSession.getId(), tempMap);
						}
					} else {
						int lap = updateMap.get(knowledgeSession.getId()).get(
								step.getBeanName());
						if ((int) ((currentMilli - startMilli) / (step
								.getUpdateInterval() * 60000)) > lap) {
							// update lap, retract specific facts, set to insert
							// facts
							updateMap.get(knowledgeSession.getId()).put(
									step.getBeanName(),
									(int) ((currentMilli - startMilli) / (step
											.getUpdateInterval() * 60000)));
							ObjectFilter objectFilter;
							if (useInterface) {
								objectFilter = new IdentifierObjectFilter(
										step.getBeanName());
							} else {
								objectFilter = new CollectionsDifferenceObjectFilter(
										step);
							}
							Collection<FactHandle> factHandles = knowledgeSession
									.getFactHandles(objectFilter);
							if (factHandles != null && !factHandles.isEmpty()) {
								for (FactHandle factHandle : factHandles) {
									commands.add(new RetractCommand(factHandle));
								}
								if (LOGGER_PERFORMANCE.isDebugEnabled())
									stopWatch
											.lap("KnowledgeSessionService",
													"After Retract Step ["
															+ step.getBeanName()
															+ "/"
															+ step.getClass()
																	.getSimpleName()
															+ "] added "
															+ factHandles
																	.size()
															+ " command(s) to retract facts");
							}
							update = true;
						}
					}
				}

				if (isNewSession || step.getType() == Type.NEW
						|| step.getTarget() == Target.GLOBALS || update) {

					if (!stepUtil.createBatchExecutionCommandFromCache(this,
							commands, message, step)) {

						if (step instanceof HqlStep) {
							stepUtil.createBatchExecutionCommand(this,
									commands, message, evaluationContext,
									(HqlStep) step, getEntityManagerFactory());
						} else {
							stepUtil.createBatchExecutionCommand(this,
									commands, message, evaluationContext,
									(ExpressionStep) step,
									getEntityManagerFactory());
						}

					}

					if (LOGGER_PERFORMANCE.isDebugEnabled())
						stopWatch.lap("KnowledgeSessionService", "After Step ["
								+ step.getBeanName() + "/"
								+ step.getClass().getSimpleName()
								+ "] executed");
				}

			}
			// Add fireAllRules command
			commands.add(new FireAllRulesCommand());
			commands.add(new HaltCommand());

			// Execute the commands prepared
			knowledgeSession
					.execute(CommandFactory.newBatchExecution(commands));

			if (LOGGER_PERFORMANCE.isTraceEnabled())
				stopWatch.lap("KnowledgeSessionService",
						"Transform-After execute command");

		} catch (Exception e) {
			LOGGER.error(
					"Error during the execution of statefull drools for the message ["
							+ message.toString() + "]", e);
		} finally {
			try {
				if (poolSize > 0) {
					pool.returnObject(knowledgeSession);
				} else {
					if (knowledgeSession != null) {
						knowledgeSession.dispose();
					}
				}
			} catch (Exception e) {
				LOGGER.error(
						"Error returning session to the pool, or disposing of session, for the message ["
								+ message.toString() + "]", e);
			}
		}

		if (LOGGER_PERFORMANCE.isInfoEnabled())
			stopWatch.stop("KnowledgeSessionService", "Done-transform");

		return message;

	}

	public PseudoStatelessKnowledgeSessionFactory getPseudoStatelessKnowledgeSessionFactory() {
		return pseudoStatelessKnowledgeSessionFactory;
	}

	public void setPseudoStatelessKnowledgeSessionFactory(
			PseudoStatelessKnowledgeSessionFactory PseudoStatelessKnowledgeSessionFactory) {
		this.pseudoStatelessKnowledgeSessionFactory = PseudoStatelessKnowledgeSessionFactory;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public List<AbstractStep> getSteps() {
		return steps;
	}

	public void setSteps(List<AbstractStep> steps) {
		this.steps = steps;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public int getDisposeInterval() {
		return disposeInterval;
	}

	public void setDisposeInterval(int disposeInterval) {
		this.disposeInterval = disposeInterval;
	}

	public boolean isUseInterface() {
		return useInterface;
	}

	public void setUseInterface(boolean useInterface) {
		this.useInterface = useInterface;
	}

}
