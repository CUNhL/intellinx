package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.drools.base.MapGlobalResolver;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.command.runtime.rule.FireAllRulesCommand;
import org.drools.command.runtime.rule.HaltCommand;
import org.drools.command.runtime.rule.RetractCommand;
import org.drools.runtime.ClassObjectFilter;
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
public class KnowledgeSessionService extends AbstractDroolsService implements
		BeanNameAware, Transformer, InitializingBean {

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private static final Logger LOGGER = LoggerFactory
			.getLogger(KnowledgeSessionService.class);

	@Autowired
	private ApplicationContext applicationContext;

	private KnowledgeSessionFactory knowledgeSessionFactory;

	private ObjectPool<StatefulKnowledgeSession> pool;

	private List<AbstractStep> steps;

	private EvaluationContext evaluationContext;

	// When

	private String when;

	private Expression calculatedWhenExpression;

	private int poolSize;

	private StepUtil stepUtil;
	
	private int updateInterval = 0;
	
	private Date lastUpdated;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(knowledgeSessionFactory);
		Assert.notNull(getBeanName(), "Bean Name is required");
		Assert.notNull(applicationContext);
		Assert.isTrue(poolSize != 0,
				"The attribute pool size shall be provided");

		stepUtil = new StepUtil(new SpelExpressionParser());

		// Prepare Steps
		if (steps != null)

			for (AbstractStep step : steps) {

				stepUtil.prepare(step);
				
				if(step.getTarget() == Target.FACT){
					Assert.notNull(step.getType(), "Type is required for steps with target FACT");
					if(step.getType() == Type.NEW){
						Assert.notNull(step.getFactClass(), "FactClass is required for steps with target FACT and type NEW");
					}
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
		pool = new StackObjectPool<StatefulKnowledgeSession>(
				knowledgeSessionFactory, getPoolSize());

		// Evaluation Context
		StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
		BeanResolver beanResolver = new BeanFactoryResolver(applicationContext);
		standardEvaluationContext.setBeanResolver(beanResolver);

		evaluationContext = standardEvaluationContext;
		
		lastUpdated = new Date();

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

			// Check if time expired and invalidate pool's map
			if(updateInterval > 0){
				Date tempDate = new Date();
				if(tempDate.getTime() - lastUpdated.getTime() > updateInterval * 60000){
					knowledgeSessionFactory.invalidatePool();
					lastUpdated = tempDate;
				}
			}

			knowledgeSession = pool.borrowObject();

			List<Command<?>> commands = new ArrayList<Command<?>>();

			boolean isNewSession = (knowledgeSession.getFactCount() == 0 && ((MapGlobalResolver)knowledgeSession.getGlobals()).getGlobals().length == 0);

			for (AbstractStep step : steps) {
				
				if(!isNewSession && step.getType() == Type.NEW && step.getTarget() == Target.FACT){
					//retract specific classes from previous runs
					if(!(step instanceof ExpressionStep)){
						LOGGER.error("Can only retract ExpressionSteps. Non expression steps cannot be set to target FACT and type NEW. Returning.");
						return message;
					}
					
					/*Object object = ((ExpressionStep)step).getExpressionsParsed().getValue(evaluationContext, message);
					if(object instanceof Collection<?>){
						Collection<?> collection = (Collection<?>) object;
						if(collection.isEmpty()){
							//screwed
							object = null;
						}
						else{
							object = collection.iterator().next();
						}
					}
					if(object != null){
						ObjectFilter objectFilter = new ClassObjectFilter(object.getClass());
						commands.add(new GetObjectsCommand(objectFilter));//can't tell from doc if this retracts or not. I'm hoping does.
					}*/
					
//					ObjectFilter objectFilter = new ClassObjectFilter(Class.forName(step.getFactClass()));
//					commands.add(new GetObjectsCommand(objectFilter));//can't tell from doc if this retracts or not. I'm hoping does.
					
					ObjectFilter objectFilter = new ClassObjectFilter(Class.forName(step.getFactClass()));
					Collection<FactHandle> factHandles = knowledgeSession.getFactHandles(objectFilter);
					if (factHandles != null && !factHandles.isEmpty()) {
						for (FactHandle factHandle : factHandles) {
							commands.add(new RetractCommand(factHandle));
						}
					}
					
					
//					Collection<FactHandle> factHandles = knowledgeSession.getFactHandles(objectFilter);
//					ObjectFilter objectFilter = new ClassFilter(objectClass);
//					if (factHandles != null && !factHandles.isEmpty()) {
//						for (FactHandle factHandle : factHandles) {
//							cmds.add(new RetractCommand(factHandle));
//						}
				}

				if (isNewSession || step.getType() == Type.NEW || step.getTarget() == Target.GLOBALS) {

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
				pool.returnObject(knowledgeSession);
			} catch (Exception e) {
				LOGGER.error(
						"Error returning session to the pool for the message ["
								+ message.toString() + "]", e);
			}
		}

		if (LOGGER_PERFORMANCE.isInfoEnabled())
			stopWatch.stop("KnowledgeSessionService", "Done-transform");

		return message;

	}

	public KnowledgeSessionFactory getKnowledgeSessionFactory() {
		return knowledgeSessionFactory;
	}

	public void setKnowledgeSessionFactory(
			KnowledgeSessionFactory knowledgeSessionFactory) {
		this.knowledgeSessionFactory = knowledgeSessionFactory;
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

	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

}
