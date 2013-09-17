package com.intellinx.us.ps.implementation.spring.service.drools.stateful;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.drools.KnowledgeBase;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class PseudoStatelessKnowledgeSessionFactory extends
		BasePoolableObjectFactory<StatefulKnowledgeSession> implements
		InitializingBean, BeanNameAware,
		PoolableObjectFactory<StatefulKnowledgeSession> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(PseudoStatelessKnowledgeSessionFactory.class);

	private String beanName;

	private KnowledgeBase knowledgeBase;

	private Map<StatefulKnowledgeSession, KnowledgeRuntimeLogger> loggers;

	private String logfolder;

	private Map<Integer, Boolean> activeMap;

	/**
	 * 
	 */
	@Override
	public StatefulKnowledgeSession makeObject() throws Exception {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Make new knowledgeSession");
		StatefulKnowledgeSession session = knowledgeBase
				.newStatefulKnowledgeSession();
		activeMap.put(session.getId(), true);
		return session;
	}

	@Override
	public void activateObject(StatefulKnowledgeSession knowledgeSession)
			throws Exception {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Activating new knowledgeSession");

		// Add the Logger
		if (LOGGER.isDebugEnabled() && getLogfolder() != null)
			createLogger(knowledgeSession);

		super.activateObject(knowledgeSession);
	}

	@Override
	public boolean validateObject(StatefulKnowledgeSession knowledgeSession) {
		if (!activeMap.get(knowledgeSession.getId())) {
			return false;
		}
		return true;
	}

	@Override
	public void passivateObject(StatefulKnowledgeSession knowledgeSession)
			throws Exception {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Passivate new knowledgeSession");

		if (loggers.containsKey(knowledgeSession))
			loggers.remove(knowledgeSession).close();
		super.passivateObject(knowledgeSession);
	}

	@Override
	public void destroyObject(StatefulKnowledgeSession knowledgeSession)
			throws Exception {

		activeMap.remove(knowledgeSession.getId());
		knowledgeSession.dispose();

		super.destroyObject(knowledgeSession);
	}

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		loggers = new HashMap<StatefulKnowledgeSession, KnowledgeRuntimeLogger>();
		activeMap = new HashMap<Integer, Boolean>();
	}

	/**
	 * 
	 * @param ksession
	 * @return
	 */
	private void createLogger(StatefulKnowledgeSession ksession) {
		if (!loggers.containsKey(ksession)) {
			KnowledgeRuntimeLogger logger = KnowledgeRuntimeLoggerFactory
					.newFileLogger(ksession, getLogfolder() + File.separator
							+ getBeanName() + "_" + ksession.hashCode() + "_"
							+ Calendar.getInstance().getTimeInMillis());
			loggers.put(ksession, logger);
		}
	}

	public void invalidatePool() {
		for (int k : activeMap.keySet()) {
			activeMap.put(k, false);
		}
	}

	/**
	 * 
	 */
	public void removeSession(int sessionId) {
		for (StatefulKnowledgeSession session : knowledgeBase
				.getStatefulKnowledgeSessions()) {
			if (session.getId() == sessionId) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Found session " + sessionId
							+ " in knowlegeBase's sessions. Removing.");
				}
				try {
					session.dispose();
					activeMap.remove(sessionId);
				} catch (Exception e) {
					if (LOGGER.isWarnEnabled()) {
						LOGGER.warn("Exception disposing of session "
								+ sessionId, e);
					}
				}
				return;
			}
		}
		if (LOGGER.isWarnEnabled()) {
			LOGGER.warn("Session " + sessionId
					+ " was NOT found in knowlegeBase's sessions.");
		}
	}

	/**
	 * 
	 */
	public void removeAllExpiredSessions() {
		if (LOGGER.isWarnEnabled()) {
			LOGGER.warn("Disposing of all knowledgeBase sessions not in pool");
		}
		for (StatefulKnowledgeSession session : knowledgeBase
				.getStatefulKnowledgeSessions()) {
			if (!activeMap.containsKey(session.getId())) {
				try {
					LOGGER.warn("Disposing session " + session.getId());
					session.dispose();
				} catch (Exception e) {
					if (LOGGER.isWarnEnabled()) {
						LOGGER.warn(
								"Exception disposing of session "
										+ session.getId(), e);
					}
				}
			}
		}
	}
	
	public StatefulKnowledgeSession getNewStatefulKnowledgeSession() throws Exception {
		StatefulKnowledgeSession session = knowledgeBase
				.newStatefulKnowledgeSession();
		return session;
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
