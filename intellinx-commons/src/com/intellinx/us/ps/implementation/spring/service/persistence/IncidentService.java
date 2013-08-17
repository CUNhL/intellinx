package com.intellinx.us.ps.implementation.spring.service.persistence;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang.StringUtils;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.integration.transformer.Transformer;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import com.intellinx.bom.entity.BasicIncident;
import com.intellinx.integration.incident.IncidentsWriter;

/**
 * 
 * @author Renato Mendes
 * 
 */
@SuppressWarnings("rawtypes")
public class IncidentService implements Transformer, InitializingBean {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private IncidentsWriter incidentsWriter;

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private boolean flush;

	private String expression;

	private Expression parsedExpression;

	private StandardEvaluationContext standardEvaluationContext;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(
				incidentsWriter,
				"the field [incidentsWriter] is required, and should point to the Intellinx IncidentWriter");

		SpelExpressionParser parser = new SpelExpressionParser();

		if (StringUtils.isEmpty(expression)) {
			expression = "payload";
		}

		parsedExpression = parser.parseExpression(expression);

		// Evaluation Context
		standardEvaluationContext = new StandardEvaluationContext();
		BeanResolver beanResolver = new BeanFactoryResolver(applicationContext);
		standardEvaluationContext.setBeanResolver(beanResolver);

	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	@ServiceActivator
	public Message<?> transform(Message<?> message) {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("IncidentService", message
					.getPayload().getClass().getName(), LOGGER_PERFORMANCE);

		try {

			Object target = parsedExpression.getValue(
					standardEvaluationContext, message);

			if (target instanceof Collection<?>) {

				List<BasicIncident> basicIncidents = (List<BasicIncident>) target;
				if (basicIncidents != null && !basicIncidents.isEmpty()) {
					if (flush) {
						EntityManager entityManager = EntityManagerFactoryUtils
								.getTransactionalEntityManager(entityManagerFactory);
						entityManager.flush();
						if (LOGGER_PERFORMANCE.isDebugEnabled())
							stopWatch.lap("IncidentService", "After Flush");

					}

					for (BasicIncident basicIncident : basicIncidents) {
						synchronized (basicIncident.getClass()) {
							incidentsWriter.write(basicIncident);
						}
						if (LOGGER_PERFORMANCE.isDebugEnabled())
							stopWatch.lap("IncidentService", basicIncident
									.getClass().getSimpleName());
					}

				}

			} else if (target instanceof BasicIncident) {
				BasicIncident basicIncident = (BasicIncident) target;
				synchronized (basicIncident.getClass()) {
					incidentsWriter.write(basicIncident);
				}
			} else {
				// Error !
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("IncidentService", "IncidentService finalized");

		return message;
	}

	public IncidentsWriter getIncidentsWriter() {
		return incidentsWriter;
	}

	public void setIncidentsWriter(IncidentsWriter incidentsWriter) {
		this.incidentsWriter = incidentsWriter;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public boolean isFlush() {
		return flush;
	}

	public void setFlush(boolean flush) {
		this.flush = flush;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

}
