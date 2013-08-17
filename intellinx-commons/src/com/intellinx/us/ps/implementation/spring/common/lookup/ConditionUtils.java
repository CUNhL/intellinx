package com.intellinx.us.ps.implementation.spring.common.lookup;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class ConditionUtils {

	/**
	 * 
	 * @param condition
	 */
	public final static void prepare(Condition condition) {
		ExpressionParser parser = new SpelExpressionParser();
		if (condition != null
				&& StringUtils.isNotBlank(condition.getExpression())) {
			List<Expression> expressions = new ArrayList<Expression>();
			expressions.add(parser.parseExpression(condition.getExpression()));
			condition.setExpressions(expressions);
		}

	}

	/**
	 * 
	 * This function returns TRUE if the CONDITION allows the processing to
	 * continue or a given message
	 * 
	 * @param message
	 * @param condition
	 * @return
	 */
	public final static boolean continueProcessing(Message<?> message,
			Condition condition) {

		// the condition is not given, so the processing shall continue
		if (condition == null)
			return true;
		else {
			// there is a given condition the processing shall be analyzed
			return condition.getExpressions().get(0)
					.getValue(message, Boolean.class);
		}

	}

}
