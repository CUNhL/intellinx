package com.intellinx.us.ps.implementation.spring.service.drools.common.step;

import com.intellinx.us.ps.implementation.spring.service.drools.common.query.LoadDataQuery;

/**
 * 
 * @author RenatoM
 * 
 */
public class HqlStep extends AbstractStep {

	private LoadDataQuery query;

	public LoadDataQuery getQuery() {
		return query;
	}

	public void setQuery(LoadDataQuery query) {
		this.query = query;
	}	

}
