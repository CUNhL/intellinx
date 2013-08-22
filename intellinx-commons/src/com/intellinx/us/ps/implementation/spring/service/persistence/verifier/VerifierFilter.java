package com.intellinx.us.ps.implementation.spring.service.persistence.verifier;

import java.util.List;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSelector;

import com.intellinx.us.ps.implementation.spring.service.persistence.verifier.step.AbstractStep;
import com.intellinx.us.ps.implementation.spring.service.persistence.verifier.step.StepUtil;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class VerifierFilter implements InitializingBean, BeanNameAware,
		MessageSelector {

	private String beanName;

	private List<AbstractStep> steps;

	private StepUtil stepUtil;

	/**
	 * 
	 */
	@Override
	public boolean accept(Message<?> message) {

		for (AbstractStep step : steps) {
			return stepUtil.calculate(step, message);
		}

		return true;

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		stepUtil = new StepUtil(null);
		stepUtil.prepare(steps);
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public List<AbstractStep> getSteps() {
		return steps;
	}

	public void setSteps(List<AbstractStep> steps) {
		this.steps = steps;
	}

}
