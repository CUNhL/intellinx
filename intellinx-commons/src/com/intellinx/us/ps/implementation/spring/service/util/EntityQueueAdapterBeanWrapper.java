package com.intellinx.us.ps.implementation.spring.service.util;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;

import com.intellinx.integration.internalqueue.AbstractInternalQueueAdapterBean;
import com.intellinx.queue.GXIQueueMessage;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class EntityQueueAdapterBeanWrapper extends
		AbstractInternalQueueAdapterBean {

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	/**
	 * 
	 * @param message
	 * @return
	 */
	protected Object getObjectFromMessage(GXIQueueMessage message) {
		try {
			ByteArrayInputStream bin = new ByteArrayInputStream(
					message.getBody());
			ObjectInputStream ois = new ObjectInputStream(bin);
			Object entity = ois.readObject();
			ois.close();
			return entity;
		} catch (Exception e) {
			throw new RuntimeException("Failed reading input queue message", e);
		}
	}

	@Override
	public Message<?> get() {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("EntityQueueAdapterBeanWrapper",
					"get", LOGGER_PERFORMANCE);

		Message<?> message = super.get();

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("EntityQueueAdapterBeanWrapper",
					"get-done ( message is null:" + (message == null) + ")");

		return message;

	}

}
