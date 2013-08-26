package com.intellinx.us.ps.implementation.spring.service.drools.stateless;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.drools.KnowledgeBase;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatelessKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class KnowledgeSessionFactory extends
		BasePoolableObjectFactory<StatelessKnowledgeSession> implements
		InitializingBean, BeanNameAware,
		PoolableObjectFactory<StatelessKnowledgeSession> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(KnowledgeSessionFactory.class);

	private String beanName;

	private KnowledgeBase knowledgeBase;

	private Map<StatelessKnowledgeSession, KnowledgeRuntimeLogger> loggers;

	private String logfolder;

	/**
	 * 
	 */
	@Override
	public StatelessKnowledgeSession makeObject() throws Exception {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Make new knowledgeSession");
		StatelessKnowledgeSession session = knowledgeBase
				.newStatelessKnowledgeSession();
		return session;
	}

	@Override
	public void activateObject(StatelessKnowledgeSession knowledgeSession)
			throws Exception {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Activating new knowledgeSession");

		// Add the Logger
		if (LOGGER.isDebugEnabled() && getLogfolder() != null)
			createLogger(knowledgeSession);

		super.activateObject(knowledgeSession);
	}

	@Override
	public void passivateObject(StatelessKnowledgeSession knowledgeSession)
			throws Exception {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Passivate new knowledgeSession");

		if (loggers.containsKey(knowledgeSession))
			loggers.remove(knowledgeSession).close();
		super.passivateObject(knowledgeSession);
	}

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		loggers = new HashMap<StatelessKnowledgeSession, KnowledgeRuntimeLogger>();
	}

	/**
	 * 
	 * @param ksession
	 * @return
	 */
	private void createLogger(StatelessKnowledgeSession ksession) {
		if (!loggers.containsKey(ksession)) {
			KnowledgeRuntimeLogger logger = KnowledgeRuntimeLoggerFactory
					.newFileLogger(ksession, getLogfolder() + File.separator
							+ getBeanName() + "_" + ksession.hashCode() + "_"
							+ Calendar.getInstance().getTimeInMillis());
			loggers.put(ksession, logger);
		}
	}

	public final KnowledgeBase getKnowledgeBase() {
		return knowledgeBase;
	}

	public final void setKnowledgeBase(KnowledgeBase knowledgeBase) {
		this.knowledgeBase = knowledgeBase;
	}

	public String getLogfolder() {
		return logfolder;
	}

	public void setLogfolder(String logfolder) {
		this.logfolder = logfolder;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

}
