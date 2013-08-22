package com.intellinx.us.ps.implementation.spring.common.lookup.step;

import java.io.Serializable;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;

/**
 * 
 * @author RenatoM
 * 
 */
public class Merge implements BeanNameAware, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5108879433606016490L;

	private String beanName;

	private MergeStrategy strategy;

	private String mergeFrom;

	private Expression mergeFromExpression;

	public MergeStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(MergeStrategy strategy) {
		this.strategy = strategy;
	}

	public String getMergeFrom() {
		return mergeFrom;
	}

	public void setMergeFrom(String mergeFrom) {
		this.mergeFrom = mergeFrom;
	}

	public Expression getMergeFromExpression() {
		return mergeFromExpression;
	}

	public void setMergeFromExpression(Expression mergeFromExpression) {
		this.mergeFromExpression = mergeFromExpression;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

}
