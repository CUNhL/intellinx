package com.intellinx.us.ps.implementation.spring.service.persistence.lookup;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.transformer.Transformer;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class MergeService implements InitializingBean, Transformer,
		BeanNameAware {

	private boolean disabled;

	private EntityManagerFactory entityManagerFactory;

	private String beanName;

	private Map<String, String> expressions;

	private Map<Expression, Expression> compiledExpressions;

	/**
	 * 
	 */
	@Override
	public Message<?> transform(Message<?> message) {

		if (isDisabled())
			return message;

		if (!compiledExpressions.isEmpty()) {

			EntityManager entityManager = EntityManagerFactoryUtils
					.getTransactionalEntityManager(entityManagerFactory);

			for (Expression condition : compiledExpressions.keySet()) {

				Expression target = compiledExpressions.get(condition);

				if (condition.getValue(message, Boolean.class)) {

					Object toAttach = target.getValue(message);

					Object attached = entityManager.merge(toAttach);

					target.setValue(message, attached);

				}

			}

		}

		return message;
	}

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		ExpressionParser parser = new SpelExpressionParser();

		compiledExpressions = new HashMap<Expression, Expression>();

		for (String condition : expressions.keySet()) {
			//
			Expression conditionExpression = parser.parseExpression(condition);
			Expression targetExpression = parser.parseExpression(expressions
					.get(condition));

			compiledExpressions.put(conditionExpression, targetExpression);

		}

	}

	public String getBeanName() {
		return beanName;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public Map<String, String> getExpressions() {
		return expressions;
	}

	public void setExpressions(Map<String, String> expressions) {
		this.expressions = expressions;
	}

	public Map<Expression, Expression> getCompiledExpressions() {
		return compiledExpressions;
	}

	public void setCompiledExpressions(
			Map<Expression, Expression> compiledExpressions) {
		this.compiledExpressions = compiledExpressions;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

}
