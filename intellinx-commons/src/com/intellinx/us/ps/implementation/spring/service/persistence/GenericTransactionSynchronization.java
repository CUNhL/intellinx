package com.intellinx.us.ps.implementation.spring.service.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.TransactionSynchronizationProcessor;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class GenericTransactionSynchronization implements
		TransactionSynchronizationProcessor, InitializingBean {

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private Map<Thread, StopWatch> performanceMap;

	private Map<UUID, Long> retryMap;

	private volatile MessageChannel temporaryChannel;

	private volatile MessageChannel retryChannel;

	private volatile MessageChannel errorChannel;

	private final static String ID = "id";

	@Override
	public void afterPropertiesSet() throws Exception {
		performanceMap = new HashMap<Thread, StopWatch>();
		retryMap = new HashMap<UUID, Long>();
	}

	@Override
	public void processBeforeCommit(IntegrationResourceHolder holder) {
		StopWatch stopWatch = getStopWatch();
		if (stopWatch != null) {
			stopWatch.start("GenericTransactionSynchronization",
					"processAfterRollback");
		}
	}

	@Override
	public void processAfterCommit(IntegrationResourceHolder holder) {
		StopWatch stopWatch = getStopWatch();
		if (stopWatch != null) {
			stopWatch.lap("GenericTransactionSynchronization",
					"processAfterCommit");
		}
	}

	@Override
	public void processAfterRollback(IntegrationResourceHolder holder) {

		StopWatch stopWatch = getStopWatch();

		if (stopWatch != null) {
			stopWatch.stop("GenericTransactionSynchronization",
					"processAfterRollback");
		}

		// Get Original message
		//
		Message<?> message = holder.getMessage();

		//
		//
		Long count = null;
		if (retryMap.containsKey(getMessageId(holder))) {
			count = retryMap.get(getMessageId(holder));
			count++;
			retryMap.put(getMessageId(holder), count);
		} else {
			// first problem
			count = 1L;
			retryMap.put(getMessageId(holder), count);
		}

		//
		if (count > 5L) {
			if (errorChannel != null)
				errorChannel.send(message, 0L);
		} else {
			if (retryChannel != null)
				retryChannel.send(message, 0L);
		}

	}

	/**
	 * 
	 * @return
	 */
	private StopWatch getStopWatch() {

		if (!LOGGER_PERFORMANCE.isDebugEnabled())
			return null;

		Thread thread = Thread.currentThread();

		if (performanceMap.containsKey(thread)) {
			return performanceMap.get(thread);
		}

		//
		StopWatch stopWatch = new Slf4JStopWatch(
				"GenericTransactionSynchronization", thread.getName(),
				LOGGER_PERFORMANCE);

		performanceMap.put(thread, stopWatch);

		return stopWatch;

	}

	/**
	 * 
	 * @param holder
	 * @return
	 */
	private UUID getMessageId(IntegrationResourceHolder holder) {
		return (UUID) holder.getMessage().getHeaders().get(ID);
	}

	public MessageChannel getTemporaryChannel() {
		return temporaryChannel;
	}

	public void setTemporaryChannel(MessageChannel temporaryChannel) {
		this.temporaryChannel = temporaryChannel;
	}

	public MessageChannel getErrorChannel() {
		return errorChannel;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public MessageChannel getRetryChannel() {
		return retryChannel;
	}

	public void setRetryChannel(MessageChannel retryChannel) {
		this.retryChannel = retryChannel;
	}

}
