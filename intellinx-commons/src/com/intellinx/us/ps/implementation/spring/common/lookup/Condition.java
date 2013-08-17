package com.intellinx.us.ps.implementation.spring.common.lookup;

import java.util.List;

import org.springframework.expression.Expression;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class Condition {

	private String expression;

	private List<Expression> expressions;

	private Behavior behavior;

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Behavior getBehavior() {
		return behavior;
	}

	public void setBehavior(Behavior behavior) {
		this.behavior = behavior;
	}

	public List<Expression> getExpressions() {
		return expressions;
	}

	public void setExpressions(List<Expression> expressions) {
		this.expressions = expressions;
	}

}
