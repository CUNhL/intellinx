package com.intellinx.us.ps.implementation.spring.service.drools.stateless;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.StatelessKnowledgeSession;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.Assert;

import com.intellinx.us.ps.implementation.spring.service.drools.AbstractDroolsService;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.AbstractStep;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.ExpressionStep;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.HqlStep;
import com.intellinx.us.ps.implementation.spring.service.drools.common.step.StepUtil;

/**
 * 
 * @author RenatoM
 * 
 */
public class KnowledgeSessionService extends AbstractDroolsService implements
		BeanNameAware, Transformer, InitializingBean {

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	@Autowired
	private ApplicationContext applicationContext;

	private KnowledgeSessionFactory knowledgeSessionFactory;

	private ObjectPool<StatelessKnowledgeSession> pool;

	private List<AbstractStep> steps;

	private com.intellinx.us.ps.implementation.infrastructure.StopWatch globalStopWatch;

	private StandardEvaluationContext standardEvaluationContext;

	// When

	private String when;

	private Expression calculatedWhenExpression;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		StepUtil stepUtil = new StepUtil();

		if (steps != null)

			for (AbstractStep step : steps) {

				stepUtil.prepare(step);

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

		Assert.notNull(knowledgeSessionFactory);

		// Pool of Drools Sessions
		pool = new StackObjectPool<StatelessKnowledgeSession>(
				knowledgeSessionFactory, 5);

		Assert.notNull(applicationContext);

		// Evaluation Context
		standardEvaluationContext = new StandardEvaluationContext();
		BeanResolver beanResolver = new BeanFactoryResolver(applicationContext);
		standardEvaluationContext.setBeanResolver(beanResolver);

	}

	/**
	 * 
	 */
	@Override
	@ServiceActivator
	public Message<?> transform(Message<?> message) {

		// When for the Bean
		if (calculatedWhenExpression != null
				&& !calculatedWhenExpression.getValue(
						standardEvaluationContext, message, Boolean.class)) {
			return message;
		}

		StepUtil stepUtil = new StepUtil();

		org.perf4j.StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("KnowledgeSessionService",
					"start-transform", LOGGER_PERFORMANCE);

		StatelessKnowledgeSession knowledgeSession = null;

		try {

			knowledgeSession = pool.borrowObject();

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch.lap("KnowledgeSessionService",
						"Transform-After Create knowledgeSession");

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch.lap("KnowledgeSessionService",
						"Transform-After prepare message");

			List<Command<?>> commands = new ArrayList<Command<?>>();
			for (AbstractStep step : steps) {
				if (step instanceof HqlStep) {
					stepUtil.createBatchExecutionCommand(this, commands,
							message, knowledgeSession,
							standardEvaluationContext, (HqlStep) step,
							getEntityManagerFactory());
				} else {
					stepUtil.createBatchExecutionCommand(this, commands,
							message, knowledgeSession,
							standardEvaluationContext, (ExpressionStep) step,
							getEntityManagerFactory());
				}
			}

			knowledgeSession
					.execute(CommandFactory.newBatchExecution(commands));

			if (LOGGER_PERFORMANCE.isDebugEnabled())
				stopWatch.lap("KnowledgeSessionService",
						"Transform-After execute command");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				pool.returnObject(knowledgeSession);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("KnowledgeSessionService", "Done-transform");

		// Handle new header information
		@SuppressWarnings("unchecked")
		Map<String, ?> map = (Map<String, ?>) message.getHeaders().get(
				"MESSAGE_HEADER");

		if (map != null && !map.isEmpty()) {
			MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
			builder.copyHeaders(map);
			return builder.build();
		}

		return message;

	}

	public KnowledgeSessionFactory getKnowledgeSessionFactory() {
		return knowledgeSessionFactory;
	}

	public void setKnowledgeSessionFactory(
			KnowledgeSessionFactory knowledgeSessionFactory) {
		this.knowledgeSessionFactory = knowledgeSessionFactory;
	}

	public final com.intellinx.us.ps.implementation.infrastructure.StopWatch getGlobalStopWatch() {
		return globalStopWatch;
	}

	public final void setGlobalStopWatch(
			com.intellinx.us.ps.implementation.infrastructure.StopWatch globalStopWatch) {
		this.globalStopWatch = globalStopWatch;
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

}
