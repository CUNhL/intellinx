package com.intellinx.us.ps.implementation.spring.job.processor;

import org.springframework.batch.item.ItemProcessor;

import com.intellinx.us.ps.implementation.spring.common.lookup.LookupEngine;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class LookupProcessor implements ItemProcessor<Object, Object> {

	private LookupEngine lookupEngine;

	/**
	 * 
	 */
	@Override
	public Object process(Object object) throws Exception {
		return lookupEngine.transform(object);
	}

	public LookupEngine getLookupEngine() {
		return lookupEngine;
	}

	public void setLookupEngine(LookupEngine lookupEngine) {
		this.lookupEngine = lookupEngine;
	}

}
