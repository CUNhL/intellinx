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

	private String expression;

	private Expression calculatedExpression;

	private String mergeFrom;

	private Expression calculatedMergeFrom;

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

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Expression getCalculatedExpression() {
		return calculatedExpression;
	}

	public void setCalculatedExpression(Expression calculatedExpression) {
		this.calculatedExpression = calculatedExpression;
	}

	public Expression getCalculatedMergeFrom() {
		return calculatedMergeFrom;
	}

	public void setCalculatedMergeFrom(Expression calculatedMergeFrom) {
		this.calculatedMergeFrom = calculatedMergeFrom;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

}
