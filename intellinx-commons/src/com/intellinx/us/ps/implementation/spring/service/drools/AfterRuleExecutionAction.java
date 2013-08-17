package com.intellinx.us.ps.implementation.spring.service.drools;

import java.util.Set;

/**
 * 
 * @author RenatoM
 * 
 */
public class AfterRuleExecutionAction {

	private String queryName;

	private Set<String> variables;

	private Class<?> objectClass;

	private String headerName;

	private boolean retract;

	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	public Set<String> getVariables() {
		return this.variables;
	}

	public void setVariables(Set<String> variables) {
		this.variables = variables;
	}

	public Class<?> getObjectClass() {
		return objectClass;
	}

	public void setObjectClass(Class<?> objectClass) {
		this.objectClass = objectClass;
	}

	public String getHeaderName() {
		return headerName;
	}

	public void setHeaderName(String headerName) {
		this.headerName = headerName;
	}

	public boolean isRetract() {
		return retract;
	}

	public void setRetract(boolean retract) {
		this.retract = retract;
	}

}
