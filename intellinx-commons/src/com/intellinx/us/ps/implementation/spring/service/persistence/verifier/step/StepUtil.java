package com.intellinx.us.ps.implementation.spring.service.persistence.verifier.step;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.expression.ExpressionParser;
import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class StepUtil {

	private ExpressionParser parser;

	/**
	 * 
	 * @param parser
	 */
	public StepUtil(ExpressionParser parser) {
		this.parser = parser;
	}

	/**
	 * 
	 * @param steps
	 */
	public void prepare(List<AbstractStep> steps) {
		for (AbstractStep step : steps) {
			prepare(step);
		}
	}

	/**
	 * 
	 * @param step
	 */
	public void prepare(AbstractStep step) {

		Assert.notNull(step.getExpression());

		if (step instanceof RegexStep) {
			RegexStep regexStep = (RegexStep) step;
			Assert.notNull(regexStep.getRegularExpression());
			regexStep.setPattern(Pattern.compile(regexStep
					.getRegularExpression()));
		} else if (step instanceof RegexStep) {

		}

		step.setParsedExpression(parser.parseExpression(step.getExpression()));

	}

	/**
	 * 
	 * @param step
	 * @return
	 */
	public boolean calculate(AbstractStep step, Message<?> message) {
		if (step instanceof ExpressionStep) {
			return calculate((ExpressionStep) step, message);
		} else if (step instanceof RegexStep) {
			return calculate((RegexStep) step, message);
		}
		return true;
	}

	/**
	 * 
	 * @param step
	 * @return
	 */
	private boolean calculate(ExpressionStep step, Message<?> message) {
		return true;
	}

	/**
	 * 
	 * @param step
	 * @return
	 */
	private boolean calculate(RegexStep step, Message<?> message) {
		String value = step.getParsedExpression().getValue(message,
				String.class);
		Matcher matcher = step.getPattern().matcher(value);
		if (!matcher.matches()) {
			return false;
		}
		return true;
	}

}
