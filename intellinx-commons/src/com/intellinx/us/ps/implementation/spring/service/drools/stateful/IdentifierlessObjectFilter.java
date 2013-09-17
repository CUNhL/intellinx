package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import org.drools.runtime.ObjectFilter;

/**
 * Filters Objects by Identifier
 */
public class IdentifierlessObjectFilter implements ObjectFilter {

	/**
	 * Returning true means the Iterator accepts, and thus returns, the current
	 * Object.
	 * 
	 * @param object
	 * @return
	 */
	public boolean accept(Object object) {
		return !IDroolsFact.class.isAssignableFrom(object.getClass())
				|| ((IDroolsFact) object).getDroolsIdentifier() == null;
	}

}