package com.intellinx.us.ps.implementation.spring.service.drools.stateless;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.drools.KnowledgeBase;
import org.drools.event.KnowledgeRuntimeEventManager;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class KnowledgeSessionFactory extends
		BasePoolableObjectFactory<CommandExecutor> implements InitializingBean,
		BeanNameAware, PoolableObjectFactory<CommandExecutor> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(KnowledgeSessionFactory.class);

	private String beanName;

	private KnowledgeBase knowledgeBase;

	private Map<KnowledgeRuntimeEventManager, KnowledgeRuntimeLogger> loggers;

	private String logfolder;

	private KnowledgeSessionFactoryMode mode;

	private boolean dispose;

	/**
	 * 
	 */
	@Override
	public CommandExecutor makeObject() throws Exception {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Make new knowledgeSession Mode:" + mode);

		switch (mode) {
		case USE_DROOLS_STATELESS:
			return knowledgeBase.newStatelessKnowledgeSession();
		case USE_COMMONS_STATELESS:
			return knowledgeBase.newStatefulKnowledgeSession();
		default:
			LOGGER.error("Mode not implemented");
			return null;
		}

	}

	@Override
	public void activateObject(CommandExecutor knowledgeSession)
			throws Exception {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Activating new knowledgeSession");

		// Add the Logger
		if (LOGGER.isDebugEnabled() && getLogfolder() != null)
			createLogger((KnowledgeRuntimeEventManager) knowledgeSession);

		super.activateObject(knowledgeSession);
	}

	/**
	 * 
	 */
	@Override
	public void passivateObject(CommandExecutor knowledgeSession)
			throws Exception {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Passivate new knowledgeSession");

		if (loggers.containsKey(knowledgeSession))
			loggers.remove(knowledgeSession).close();

		//
		if (knowledgeSession instanceof StatefulKnowledgeSession) {
			StatefulKnowledgeSession session = (StatefulKnowledgeSession) knowledgeSession;
			// Clean Facts
			for (FactHandle factHandle : session.getFactHandles()) {
				session.retract(factHandle);
			}
		}

		super.passivateObject(knowledgeSession);
	}

	/**
	 * 
	 */
	@Override
	public void destroyObject(CommandExecutor obj) throws Exception {
		if (obj instanceof StatefulKnowledgeSession && dispose) {
			((StatefulKnowledgeSession) obj).dispose();
		}
		super.destroyObject(obj);
	}

	/**
	 * 
	 */
	@Override
	public boolean validateObject(CommandExecutor obj) {
		return super.validateObject(obj);
	}

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(mode, "Mode is required");
		loggers = new HashMap<KnowledgeRuntimeEventManager, KnowledgeRuntimeLogger>();
	}

	/**
	 * 
	 * @param ksession
	 * @return
	 */
	private void createLogger(KnowledgeRuntimeEventManager eventManager) {
		if (!loggers.containsKey(eventManager)) {
			KnowledgeRuntimeLogger logger = KnowledgeRuntimeLoggerFactory
					.newFileLogger(eventManager, getLogfolder()
							+ File.separator + getBeanName() + "_"
							+ eventManager.hashCode() + "_"
							+ Calendar.getInstance().getTimeInMillis());
			loggers.put(eventManager, logger);
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

	public KnowledgeSessionFactoryMode getMode() {
		return mode;
	}

	public void setMode(KnowledgeSessionFactoryMode mode) {
		this.mode = mode;
	}

	/**
	 * 
	 * @param sessionId
	 */
	public void clean(int sessionId) {
		for (StatefulKnowledgeSession session : knowledgeBase
				.getStatefulKnowledgeSessions()) {
			if (session.getId() == sessionId) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Found session " + sessionId
							+ " in knowlegeBase's sessions. Removing.");
				}
				try {
					session.dispose();
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
	public void cleanExprired() {
		if (LOGGER.isWarnEnabled()) {
			LOGGER.warn("Disposing of all "
					+ knowledgeBase.getStatefulKnowledgeSessions().size()
					+ " in the knowledgeBase");
		}
		for (StatefulKnowledgeSession session : knowledgeBase
				.getStatefulKnowledgeSessions()) {
			try {
				LOGGER.warn("Disposing session " + session.getId());
				session.dispose();
			} catch (Exception e) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn(
							"Exception disposing of session " + session.getId(),
							e);
				}
			}

		}
	}

	public boolean isDispose() {
		return dispose;
	}

	public void setDispose(boolean dispose) {
		this.dispose = dispose;
	}

}
