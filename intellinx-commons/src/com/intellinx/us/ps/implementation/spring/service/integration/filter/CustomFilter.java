package com.intellinx.us.ps.implementation.spring.service.integration.filter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSelector;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class CustomFilter implements MessageSelector, InitializingBean {

	private String expression;

	private Expression calculatedExpression;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();
		calculatedExpression = parser.parseExpression(expression);
	}

	/**
	 * 
	 */
	@Override
	public boolean accept(Message<?> message) {
		boolean result = calculatedExpression.getValue(message, Boolean.class);
		return result;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

}
