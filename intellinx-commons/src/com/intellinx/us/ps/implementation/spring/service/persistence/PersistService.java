package com.intellinx.us.ps.implementation.spring.service.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.transformer.Transformer;

import com.intellinx.us.ps.implementation.spring.common.lookup.Condition;
import com.intellinx.us.ps.implementation.spring.common.lookup.ConditionUtils;
import com.intellinx.us.ps.implementation.spring.service.persistence.common.AbstractPersistenceService;

/**
 * 
 * @author RenatoM
 * 
 */
@SuppressWarnings("rawtypes")
public class PersistService extends AbstractPersistenceService implements
		InitializingBean, Transformer {

	private List<String> expressions;

	private List<Expression> expressionsObj;

	private Condition condition;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		super.setPersist(true);

		// Pre-load Condition
		ConditionUtils.prepare(condition);

		// Pre-load Expressions
		expressionsObj = new ArrayList<Expression>();
		ExpressionParser parser = new SpelExpressionParser();
		if (expressions != null && !expressions.isEmpty()) {
			for (String expressionStr : expressions)
				expressionsObj.add(parser.parseExpression(expressionStr));
		} else {
			expressionsObj.add(parser.parseExpression("payload"));
		}

	}

	/**
	 * 
	 */
	@ServiceActivator
	public Message<?> transform(Message<?> message) {

		if (!ConditionUtils.continueProcessing(message, condition))
			return message;

		EntityManager entityManager = getTransactionalEntityManager();

		for (Expression expression : expressionsObj) {
			Object o = expression.getValue(message);
			persistObject(entityManager, o);
		}

		return message;
	}

	/**
	 * 
	 * @param entityManager
	 * @param o
	 */
	@SuppressWarnings("unchecked")
	private void persistObject(EntityManager entityManager, Object o) {
		if (o != null) {
			if (o instanceof Collection<?>) {
				Collection<?> toPersistObjects = (Collection<?>) o;
				for (Object object : toPersistObjects) {
					persist(entityManager, object, null);
				}
			} else {
				persist(entityManager, o, null);
			}
		}
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public List<String> getExpressions() {
		return expressions;
	}

	public void setExpressions(List<String> expressions) {
		this.expressions = expressions;
	}

}
