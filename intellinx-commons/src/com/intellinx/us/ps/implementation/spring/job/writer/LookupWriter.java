package com.intellinx.us.ps.implementation.spring.job.writer;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

import com.intellinx.us.ps.implementation.spring.common.lookup.LookupEngine;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class LookupWriter implements ItemWriter<Object> {

	private LookupEngine lookupEngine;

	@Override
	public void write(List<? extends Object> list) throws Exception {

		for (Object object : list) {
			lookupEngine.transform(object);
		}

	}

	public LookupEngine getLookupEngine() {
		return lookupEngine;
	}

	public void setLookupEngine(LookupEngine lookupEngine) {
		this.lookupEngine = lookupEngine;
	}

}
