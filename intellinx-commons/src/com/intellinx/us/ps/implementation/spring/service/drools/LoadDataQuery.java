package com.intellinx.us.ps.implementation.spring.service.drools;

import java.util.List;
import java.util.Map;

import org.drools.runtime.rule.FactHandle;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;

/**
 * 
 * @author RenatoM
 * 
 */
public class LoadDataQuery implements BeanNameAware {

	private String beanName;

	private String query;

	private Map<String, String> parameters;

	private Map<String, Expression> parametersExpressions;

	private boolean readOnly;

	private boolean cacheable;

	private boolean active;

	private String entryPoint;

	private Integer refreshRate;

	private RefreshRateType refreshRateType;

	private List<FactHandle> factHandles;

	private String cacheRegion;

	public String getQuery() {
		return this.query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Map<String, String> getParameters() {
		return this.parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public boolean isReadOnly() {
		return this.readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isCacheable() {
		return this.cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Integer getRefreshRate() {
		return refreshRate;
	}

	public void setRefreshRate(Integer refreshRate) {
		this.refreshRate = refreshRate;
	}

	public RefreshRateType getRefreshRateType() {
		return refreshRateType;
	}

	public void setRefreshRateType(RefreshRateType refreshRateType) {
		this.refreshRateType = refreshRateType;
	}

	public Map<String, Expression> getParametersExpressions() {
		return parametersExpressions;
	}

	public void setParametersExpressions(
			Map<String, Expression> parametersExpressions) {
		this.parametersExpressions = parametersExpressions;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getEntryPoint() {
		return entryPoint;
	}

	public void setEntryPoint(String entryPoint) {
		this.entryPoint = entryPoint;
	}

	public List<FactHandle> getFactHandles() {
		return factHandles;
	}

	public void setFactHandles(List<FactHandle> factHandles) {
		this.factHandles = factHandles;
	}

	public String getCacheRegion() {
		return cacheRegion;
	}

	public void setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion;
	}

}
