package com.intellinx.us.ps.implementation.spring.common.lookup.step;

import java.io.Serializable;
import java.util.Map;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;

/**
 * 
 * @author RenatoM
 * 
 */
public class NaturalKeyStep extends AbstractHibernateStep implements
		BeanNameAware, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Map<String, String> parameters;

	private Expression sizeExpression;

	private Map<String, Map<Long, Expression>> targetObjectsExpressions;

	private Map<String, Expression> targetObjectsOriginalArrayExpressions;

	private Map<Long, Expression> whenExpressions;

	private Map<Long, Map<String, Expression>> parameterExpressions;

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public Expression getSizeExpression() {
		return sizeExpression;
	}

	public void setSizeExpression(Expression sizeExpression) {
		this.sizeExpression = sizeExpression;
	}

	public Map<Long, Expression> getWhenExpressions() {
		return whenExpressions;
	}

	public void setWhenExpressions(Map<Long, Expression> whenExpressions) {
		this.whenExpressions = whenExpressions;
	}

	public Map<Long, Map<String, Expression>> getParameterExpressions() {
		return parameterExpressions;
	}

	public void setParameterExpressions(
			Map<Long, Map<String, Expression>> parameterExpressions) {
		this.parameterExpressions = parameterExpressions;
	}

	public Map<String, Expression> getTargetObjectsOriginalArrayExpressions() {
		return targetObjectsOriginalArrayExpressions;
	}

	public void setTargetObjectsOriginalArrayExpressions(
			Map<String, Expression> targetObjectsOriginalArrayExpressions) {
		this.targetObjectsOriginalArrayExpressions = targetObjectsOriginalArrayExpressions;
	}

	public Map<String, Map<Long, Expression>> getTargetObjectsExpressions() {
		return targetObjectsExpressions;
	}

	public void setTargetObjectsExpressions(
			Map<String, Map<Long, Expression>> targetObjectsExpressions) {
		this.targetObjectsExpressions = targetObjectsExpressions;
	}

}
