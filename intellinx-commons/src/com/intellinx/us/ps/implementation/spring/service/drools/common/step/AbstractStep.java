package com.intellinx.us.ps.implementation.spring.service.drools.common.step;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;

import com.intellinx.us.ps.implementation.spring.service.common.cache.IApplicationCache;

/**
 * 
 * @author RenatoM
 * 
 */
public abstract class AbstractStep implements BeanNameAware {

	private String beanName;

	private Target target;
	
	private Type type;

	private String parameter;

	private String when;

	private Expression whenExpressionParsed;

	private IApplicationCache<?> applicationCache;

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

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public IApplicationCache<?> getApplicationCache() {
		return applicationCache;
	}

	public void setApplicationCache(IApplicationCache<?> applicationCache) {
		this.applicationCache = applicationCache;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

}
