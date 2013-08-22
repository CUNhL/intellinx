package com.intellinx.us.ps.implementation.spring.service.persistence.verifier.step;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class AbstractStep implements BeanNameAware {

	private String beanName;

	private String expression;

	private Expression parsedExpression;

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Expression getParsedExpression() {
		return parsedExpression;
	}

	public void setParsedExpression(Expression parsedExpression) {
		this.parsedExpression = parsedExpression;
	}

}
