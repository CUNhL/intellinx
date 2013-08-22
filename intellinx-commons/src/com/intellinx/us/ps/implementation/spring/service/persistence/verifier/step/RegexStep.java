package com.intellinx.us.ps.implementation.spring.service.persistence.verifier.step;

import java.util.regex.Pattern;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class RegexStep extends AbstractStep {

	private String regularExpression;

	private Pattern pattern;

	public String getRegularExpression() {
		return regularExpression;
	}

	public void setRegularExpression(String regularExpression) {
		this.regularExpression = regularExpression;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

}
