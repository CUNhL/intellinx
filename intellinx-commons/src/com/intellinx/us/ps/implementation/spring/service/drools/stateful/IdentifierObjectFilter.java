package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import org.drools.runtime.ObjectFilter;

/**
 * Filters Objects by Identifier
 */
public class IdentifierObjectFilter implements ObjectFilter {

	//private String identifier;

	/**
	 * The Allowed Identifier
	 * 
	 * @param identifier
	 */
//	public IdentifierObjectFilter(String identifier) {
//		this.identifier = identifier;
//	}

	/**
	 * Returning true means the Iterator accepts, and thus returns, the current
	 * Object.
	 * 
	 * @param object
	 * @return
	 */
	public boolean accept(Object object) {
		// return this.clazz.isAssignableFrom( object.getClass() );

		/*
		 * if(IDroolsFact.class.isInstance(object)){ System.out.println(
		 * "Is Instance Of!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
		 * ); }
		 */

		
//		return IDroolsFact.class.isAssignableFrom(object.getClass())
//				&& this.identifier != null
//				&& this.identifier.equals(((IDroolsFact) object)
//						.getDroolsIdentifier());
		
		return !IDroolsFact.class.isAssignableFrom(object.getClass())
				|| ((IDroolsFact) object).getDroolsIdentifier() == null;
	}

}