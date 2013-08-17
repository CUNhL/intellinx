package com.intellinx.us.ps.implementation.spring.job.tasklet;

import javax.persistence.EntityManagerFactory;

import org.drools.runtime.StatelessKnowledgeSession;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * @author RenatoM
 * 
 */
public class DroolsTasklet implements Tasklet, InitializingBean {

	private EntityManagerFactory entityManagerFactory;

	private String hql;

	private StatelessKnowledgeSession knowledgeSession;

	public void afterPropertiesSet() throws Exception {
	}

	public RepeatStatus execute(StepContribution arg0, ChunkContext arg1)
			throws Exception {
		/* 35 */return null;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		/* 39 */return this.entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		/* 44 */this.entityManagerFactory = entityManagerFactory;
	}

	public String getHql() {
		/* 48 */return this.hql;
	}

	public void setHql(String hql) {
		/* 52 */this.hql = hql;
	}

	public StatelessKnowledgeSession getKnowledgeSession() {
		/* 56 */return this.knowledgeSession;
	}

	public void setKnowledgeSession(StatelessKnowledgeSession knowledgeSession) {
		/* 60 */this.knowledgeSession = knowledgeSession;
	}
}
