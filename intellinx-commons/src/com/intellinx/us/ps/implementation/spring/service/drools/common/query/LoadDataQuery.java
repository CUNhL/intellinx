package com.intellinx.us.ps.implementation.spring.service.drools.common.query;

import java.io.Serializable;
import java.util.Map;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;

/**
 * 
 * @author RenatoM
 * 
 */
public class LoadDataQuery implements BeanNameAware, Serializable {

	private static final long serialVersionUID = 1L;

	private String beanName;

	private String query;

	private Map<String, String> parameters;

	private Map<String, Expression> parametersExpressions;

	private boolean readOnly;

	private boolean cacheable;

	private String cacheRegion;

	private ResultStore resultStore;

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public Map<String, Expression> getParametersExpressions() {
		return parametersExpressions;
	}

	public void setParametersExpressions(
			Map<String, Expression> parametersExpressions) {
		this.parametersExpressions = parametersExpressions;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public String getCacheRegion() {
		return cacheRegion;
	}

	public void setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion;
	}

	public ResultStore getResultStore() {
		return resultStore;
	}

	public void setResultStore(ResultStore resultStore) {
		this.resultStore = resultStore;
	}

}
