package com.intellinx.us.ps.implementation.spring.service.drools.common.step;

import org.springframework.expression.Expression;

/**
 * 
 * @author RenatoM
 * 
 */
public class ExpressionStep extends AbstractStep {

	private String expression;

	private Expression expressionsParsed;

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Expression getExpressionsParsed() {
		return expressionsParsed;
	}

	public void setExpressionsParsed(Expression expressionsParsed) {
		this.expressionsParsed = expressionsParsed;
	}

}
