package com.intellinx.us.ps.implementation.spring.common.lookup.step;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;

import com.intellinx.us.ps.implementation.spring.common.lookup.Behavior;

/**
 * 
 * @author RenatoM
 * 
 */
public abstract class AbstractStep implements BeanNameAware, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String beanName;

	private String when;

	private Behavior whenNotMetBehavior;

	private String targetObject;

	private List<String> targetObjects;

	private boolean persistIfNotFound;

	private Expression sizeExpression;

	private Map<String, Map<Long, Expression>> targetObjectsExpressions;

	private Map<String, Expression> targetObjectsOriginalArrayExpressions;

	private Map<Long, Expression> whenExpressions;

	private Map<Long, Map<String, Expression>> parameterExpressions;

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public String getTargetObject() {
		return targetObject;
	}

	public void setTargetObject(String targetObject) {
		this.targetObject = targetObject;
	}

	public boolean isPersistIfNotFound() {
		return persistIfNotFound;
	}

	public void setPersistIfNotFound(boolean persistIfNotFound) {
		this.persistIfNotFound = persistIfNotFound;
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

	public Behavior getWhenNotMetBehavior() {
		return whenNotMetBehavior;
	}

	public void setWhenNotMetBehavior(Behavior whenNotMetBehavior) {
		this.whenNotMetBehavior = whenNotMetBehavior;
	}

	public List<String> getTargetObjects() {
		return targetObjects;
	}

	public void setTargetObjects(List<String> targetObjects) {
		this.targetObjects = targetObjects;
	}

	public Map<String, Map<Long, Expression>> getTargetObjectsExpressions() {
		return targetObjectsExpressions;
	}

	public void setTargetObjectsExpressions(
			Map<String, Map<Long, Expression>> targetObjectsExpressions) {
		this.targetObjectsExpressions = targetObjectsExpressions;
	}

}
