package com.intellinx.us.ps.implementation.spring.service.persistence.lookup;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class EntityPersistService implements InitializingBean, Transformer,
		BeanNameAware {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(EntityPersistService.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private String beanName;

	private EntityManagerFactory entityManagerFactory;

	private LookupService lookupService;

	/**
	 * 
	 */
	@Override
	@ServiceActivator
	public Message<?> transform(Message<?> message) {
		Message<?> payloadMessage = null;

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isInfoEnabled())
			stopWatch = new Slf4JStopWatch("LookupService", message
					.getPayload().getClass().getName(), LOGGER_PERFORMANCE);

		if (message.getPayload() instanceof Collection) {
			List<?> list = (List<?>) message.getPayload();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Multiple Messages being processed ("
						+ list.size() + ")");
			}

			Object payload = list.get(0);
			MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
			payloadMessage = builder.copyHeaders(message.getHeaders()).build();
		} else {
			payloadMessage = message;
		}

		try {
			lookupService.transform(payloadMessage, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (stopWatch != null)
			stopWatch.stop("Done");

		return message;
	}

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
	}

	/**
	 * 
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public LookupService getLookupService() {
		return lookupService;
	}

	public void setLookupService(LookupService lookupService) {
		this.lookupService = lookupService;
	}

	public String getBeanName() {
		return beanName;
	}

}
