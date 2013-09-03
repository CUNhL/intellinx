package com.intellinx.us.ps.implementation.spring.common.lookup.step;

/**
 * 
 * How it works with LISTS ????
 * 
 * @author Renato Mendes
 * 
 */
public enum MergeStrategy {

	/**
	 * No field will update records
	 */
	DO_NOT_MERGE_FIELDS,

	/**
	 * 
	 * basically merge all fields that has different values
	 * 
	 * if the field contain value > message updates record if the field does not
	 * contain value > message updates record, but the id fields
	 */
	MERGE_ALL_FIELDS,

	/**
	 * if the field on the target object contains value > message updates record<br>
	 * if the field does not contains value > message DOES NOT update record
	 * 
	 */
	MERGE_NON_NULL_TARGET_FIELDS,

	/**
	 * if the field on the source object contains value > message updates record<br>
	 * if the field does not contains value > message DOES NOT update record
	 * 
	 */
	MERGE_NON_NULL_SOURCE_FIELDS,
	
	/**
	 * if the field on the source object contains value > message updates record<br>
	 * if the field does not contains value > message DOES NOT update record
	 * field shall not be a collection
	 */
	MERGE_NON_NULL_NON_COLLECTIONS_SOURCE_FIELDS,

	MERGE_NULL_TARGET_FIELDS,

	/**
	 * 
	 */
	USE_FIELD_BY_FIELD_STRATEGY,

	/**
	 * 
	 */
	FIELD_STRATEGY_MERGE_ALLWAYS, FIELD_STRATEGY_MERGE_IF_NOT_NULL

}
