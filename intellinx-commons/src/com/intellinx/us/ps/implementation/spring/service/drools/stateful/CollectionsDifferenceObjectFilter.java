package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.drools.runtime.ObjectFilter;

import com.intellinx.us.ps.implementation.spring.service.drools.common.step.AbstractStep;

/**
 * Filters Objects by symmetric difference of given collections
 */
public class CollectionsDifferenceObjectFilter implements ObjectFilter {

	// private List<Collection<?>> collections;

	private Collection<Object> objects;

	/**
	 * The steps that may contain collections of objects
	 * 
	 * @param steps
	 */
	public CollectionsDifferenceObjectFilter(Collection<Object> objects,
			List<AbstractStep> steps) {
		this.objects = new HashSet<Object>();
		this.objects.addAll(objects);
		for (AbstractStep step : steps) {
			if (step.getObjects() != null) {
				this.objects.removeAll(step.getObjects());
			}
		}
	}

	/**
	 * The steps that may contain collections of objects
	 * 
	 * @param steps
	 */
	@SuppressWarnings("unchecked")
	public CollectionsDifferenceObjectFilter(AbstractStep step) {
		if (step.getObjects() != null) {
			this.objects = (Collection<Object>)step.getObjects();
		}
	}

	/**
	 * Returning true means the Iterator accepts, and thus returns, the current
	 * Object.
	 * 
	 * @param object
	 * @return
	 */
	public boolean accept(Object object) {

		return objects.contains(object);

	}

}