package com.intellinx.us.ps.implementation.spring.service.drools.agenda;

import java.util.List;

import org.drools.runtime.rule.Activation;
import org.drools.runtime.rule.AgendaFilter;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class ConfigurableAgendaFilter implements AgendaFilter {

	public List<String> activeRules;

	@Override
	public boolean accept(Activation activation) {
		return true;
	}

	public final List<String> getActiveRules() {
		return activeRules;
	}

	public final void setActiveRules(List<String> activeRules) {
		this.activeRules = activeRules;
	}

}
