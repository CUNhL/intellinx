package com.intellinx.us.ps.implementation.spring.service.drools.common.query;

import java.util.List;

import org.drools.runtime.rule.FactHandle;

/**
 * 
 * @author Renato Mendes
 *
 */
public class ResultStore {

	private RefreshRateType refreshRateType;

	private List<FactHandle> factHandles;

	private Integer refreshRate;

	public RefreshRateType getRefreshRateType() {
		return refreshRateType;
	}

	public void setRefreshRateType(RefreshRateType refreshRateType) {
		this.refreshRateType = refreshRateType;
	}

	public List<FactHandle> getFactHandles() {
		return factHandles;
	}

	public void setFactHandles(List<FactHandle> factHandles) {
		this.factHandles = factHandles;
	}

	public Integer getRefreshRate() {
		return refreshRate;
	}

	public void setRefreshRate(Integer refreshRate) {
		this.refreshRate = refreshRate;
	}

}
