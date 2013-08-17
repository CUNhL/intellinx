package com.intellinx.us.ps.implementation.spring.service.drools.common.step;

import org.springframework.expression.Expression;

/**
 * 
 * @author RenatoM
 * 
 */
public abstract class AbstractStep {

	private Target target;

	private String parameter;

	private String when;

	private Expression whenExpressionParsed;

	public Target getTarget() {
		return target;
	}

	public void setTarget(Target target) {
		this.target = target;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public Expression getWhenExpressionParsed() {
		return whenExpressionParsed;
	}

	public void setWhenExpressionParsed(Expression whenExpressionParsed) {
		this.whenExpressionParsed = whenExpressionParsed;
	}

}
