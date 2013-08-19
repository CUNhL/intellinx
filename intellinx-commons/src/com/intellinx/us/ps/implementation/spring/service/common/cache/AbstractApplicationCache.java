package com.intellinx.us.ps.implementation.spring.service.common.cache;

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
import org.springframework.util.Assert;

/**
 * 
 * @author Renato Mendes
 * 
 * @param <T>
 */
public abstract class AbstractApplicationCache<T> implements
		IApplicationCache<T>, InitializingBean {

	private String key;

	private Expression keyExpression;

	private SpelExpressionParser parser;

	@Autowired
	private ApplicationContext applicationContext;

	private EvaluationContext evaluationContext;

	/**
	 * 
	 * @param parser
	 */
	public AbstractApplicationCache(SpelExpressionParser parser) {
		this.parser = parser;
	}

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		// Assert block
		Assert.notNull(parser);
		Assert.notNull(key);

		// Start key Expression
		keyExpression = parser.parseExpression(key);

		// Evaluation Context
		StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
		BeanResolver beanResolver = new BeanFactoryResolver(applicationContext);
		standardEvaluationContext.setBeanResolver(beanResolver);

		evaluationContext = standardEvaluationContext;

	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	protected Object keyValue(Message<T> message) {
		return keyExpression.getValue(evaluationContext, message);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Expression getKeyExpression() {
		return keyExpression;
	}

	public void setKeyExpression(Expression keyExpression) {
		this.keyExpression = keyExpression;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
