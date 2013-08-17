package com.intellinx.us.ps.implementation.spring.common.lookup;

import java.util.Map;

import org.springframework.expression.Expression;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class Behavior {

	private String expression;

	private BehaviorType type;

	private Map<Long, Expression> whenNotMetExpressions;

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Map<Long, Expression> getWhenNotMetExpressions() {
		return whenNotMetExpressions;
	}

	public void setWhenNotMetExpressions(
			Map<Long, Expression> whenNotMetExpressions) {
		this.whenNotMetExpressions = whenNotMetExpressions;
	}

	public BehaviorType getType() {
		return type;
	}

	public void setType(BehaviorType type) {
		this.type = type;
	}

}
