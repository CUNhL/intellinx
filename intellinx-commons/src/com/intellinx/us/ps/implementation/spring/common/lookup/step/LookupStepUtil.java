package com.intellinx.us.ps.implementation.spring.common.lookup.step;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.util.Assert;

import com.intellinx.us.ps.implementation.spring.service.persistence.lookup.LookupService;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class LookupStepUtil {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(LookupStepUtil.class);

	private ExpressionParser parser;

	/**
	 * 
	 * @param parser
	 */
	public LookupStepUtil(ExpressionParser parser) {
		this.parser = parser;
	}

	/**
	 * 
	 * @param step
	 */
	public void handleStep(HqlStep step) {

		Expression expression = null;

		// --------------------
		// Initialize Objects
		step.setTargetObjectsExpressions(new HashMap<String, Map<Long, Expression>>());
		step.setTargetObjectsOriginalArrayExpressions(new HashMap<String, Expression>());

		// Check target Objects, and add to the List of TargetObjects
		if (step.getTargetObjects() == null) {
			step.setTargetObjects(new ArrayList<String>());
		}

		if (step.getTargetObject() != null) {
			step.getTargetObjects().add(step.getTargetObject());
		}

		//
		for (String targetObject : step.getTargetObjects()) {

			Map<Long, Expression> map = new HashMap<Long, Expression>();

			if (!targetObject.contains(LookupService.POUND)) {

				expression = parser.parseExpression(targetObject);
				map.put(0L, expression);

				step.getTargetObjectsOriginalArrayExpressions().put(
						targetObject, null);

			} else {

				String targetObjectsOriginalArray = getTargetObjectOriginalArray(targetObject);

				expression = parser.parseExpression(targetObjectsOriginalArray);

				step.getTargetObjectsOriginalArrayExpressions().put(
						targetObject, expression);

				for (long j = 0; j < 1000; j++) {

					expression = parser.parseExpression(targetObject.replace(
							LookupService.POUND, String.valueOf(j)));
					map.put(j, expression);

				}

			}

			step.getTargetObjectsExpressions().put(targetObject, map);

		}

		// --------------------
		if (step.getWhen() != null) {
			step.setWhenExpressions(new HashMap<Long, Expression>());
			for (long j = 0; j < 1000; j++) {
				if (!step.getWhen().contains(LookupService.POUND)) {
					expression = parser.parseExpression(step.getWhen());
					step.getWhenExpressions().put(j, expression);
					break;
				} else {
					expression = parser.parseExpression(step.getWhen().replace(
							LookupService.POUND, String.valueOf(j)));
					step.getWhenExpressions().put(j, expression);
				}
			}
		}

		// --------------------
		if (step.getWhenNotMetBehavior() != null
				&& StringUtils.isNotBlank(step.getWhenNotMetBehavior()
						.getExpression())) {

			step.getWhenNotMetBehavior().setWhenNotMetExpressions(
					new HashMap<Long, Expression>());
			for (long j = 0; j < 1000; j++) {
				if (!step.getWhenNotMetBehavior().getExpression()
						.contains(LookupService.POUND)) {
					expression = parser.parseExpression(step
							.getWhenNotMetBehavior().getExpression());
					step.getWhenNotMetBehavior().getWhenNotMetExpressions()
							.put(j, expression);
					break;
				} else {
					expression = parser.parseExpression(step
							.getWhenNotMetBehavior().getExpression()
							.replace(LookupService.POUND, String.valueOf(j)));
					step.getWhenNotMetBehavior().getWhenNotMetExpressions()
							.put(j, expression);
				}
			}

		} else if (step.getWhenNotMetBehavior() != null) {

			Assert.notNull(step.getWhenNotMetBehavior().getType(),
					"Type is required for WhenNotMetBehavior");

		}

		// --------------------
		step.setParameterExpressions(new HashMap<Long, Map<String, Expression>>());
		for (long j = 0; j < 1000; j++) {
			Map<String, Expression> expressions = new HashMap<String, Expression>();
			for (String key : step.getParameters().keySet()) {
				String sp = step.getParameters().get(key)
						.replace(LookupService.POUND, String.valueOf(j));
				expressions.put(key, parser.parseExpression(sp));
			}
			step.getParameterExpressions().put(j, expressions);
		}

		// --------------------

		// check readonly
		if (step.getMerge() != null) {

			switch (step.getMerge().getStrategy()) {
			case DO_NOT_MERGE_FIELDS:
				step.setReadOnly(true);
				break;
			case FIELD_STRATEGY_MERGE_ALLWAYS:
			case MERGE_ALL_FIELDS:
			case MERGE_NON_NULL_SOURCE_FIELDS:
			case MERGE_NULL_TARGET_FIELDS:
			case FIELD_STRATEGY_MERGE_IF_NOT_NULL:
			default:
				if (step.getMerge().getExpression() == null) {
					Assert.isTrue(!step.isReadOnly(),
							"Using Merge Strategies, ReadOnly property should be set to false");
				}
				break;
			}

			// Calculate merge from
			Assert.notNull(step.getMerge().getMergeFrom(),
					"At least one expression of Merge From should be Defined");
			step.getMerge().setCalculatedMergeFrom(
					parser.parseExpression(step.getMerge().getMergeFrom()));

			// Calculate expression
			if (step.getMerge().getExpression() != null)
				step.getMerge()
						.setCalculatedExpression(
								parser.parseExpression(step.getMerge()
										.getExpression()));

		}

	}

	/**
	 * 
	 * @param targetobject
	 * @return
	 */
	private String getTargetObjectOriginalArray(String targetObject) {
		return targetObject.substring(0,
				targetObject.indexOf(LookupService.POUND) - 1);
	}

	/**
	 * 
	 * @param source
	 * @param target
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public static void merge(Object source, Object target, Merge merge)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {

		// Check if the type of objects are the same
		if (!source.getClass().equals(target.getClass())) {
			LOGGER.error("Merging objects from different classes: source["
					+ source.getClass().getSimpleName() + "], target["
					+ target.getClass().getSimpleName() + "]");
			return;
		}

		// List<String> ignoredProperties = new ArrayList<String>();
		PropertyDescriptor[] propertyDescriptors = BeanUtils
				.getPropertyDescriptors(source.getClass());

		//
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {

			// Find id using annotation
			// if (AnnotationUtils.getAnnotation(propertyDescriptor.get,
			// annotationType)

			// TODO Use annotation to identify the real id field
			if (propertyDescriptor.getName().toLowerCase().equals("id")) {
				// ignoredProperties.add(propertyDescriptor.getName());
				continue;
			} else if (propertyDescriptor.getName().toLowerCase()
					.equals("class")) {
				continue;
			}

			Object[] valueObjects = new Object[1];
			Object toReplace = null;

			Method readMethod = propertyDescriptor.getReadMethod();
			Method writeMethod = propertyDescriptor.getWriteMethod();

			switch (merge.getStrategy()) {

			case MERGE_NON_NULL_TARGET_FIELDS:
				// find that the value is null and not merge adding to the
				// ignored list
				valueObjects[0] = readMethod.invoke(target, new Object[0]);
				if (valueObjects[0] != null) {
					writeMethod.invoke(target, valueObjects);
				}
				break;

			case MERGE_NULL_TARGET_FIELDS:
				// find that the value is null and not merge adding to the
				// ignored list
				valueObjects[0] = readMethod.invoke(target, new Object[0]);
				if (valueObjects[0] == null) {
					writeMethod.invoke(target, valueObjects);
				}
				break;

			case MERGE_NON_NULL_SOURCE_FIELDS:
				// find that the value is null and not merge adding to the
				// ignored list
				valueObjects[0] = readMethod.invoke(source, new Object[0]);
				if (valueObjects[0] != null) {
					writeMethod.invoke(target, valueObjects);
				}
				break;

			case MERGE_NON_NULL_NON_COLLECTIONS_SOURCE_FIELDS:

				valueObjects[0] = readMethod.invoke(source, new Object[0]);
				toReplace = readMethod.invoke(target, new Object[0]);
				if (valueObjects[0] != null
						&& !(valueObjects[0] instanceof Collection)
						&& !valueObjects[0].equals(toReplace)) {
					writeMethod.invoke(target, valueObjects);
				}
				break;

			case MERGE_ALL_FIELDS:
				// All values will be merged
				valueObjects[0] = readMethod.invoke(source, new Object[0]);
				toReplace = readMethod.invoke(target, new Object[0]);
				if (!EqualsBuilder.reflectionEquals(valueObjects[0], toReplace,
						false)) {
					writeMethod.invoke(target, valueObjects);
				}
				break;
			default:

				LOGGER.warn("The Merge strategy [" + merge.getStrategy()
						+ "] was not implemented, no action will be taken.");

				break;
			}

		}

	}
}
