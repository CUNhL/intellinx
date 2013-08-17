package com.intellinx.us.ps.implementation.spring.service.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.transaction.TransactionSystemException;

/**
 * 
 * @author RenatoM
 * 
 */
public class RetryTransformer implements InitializingBean, Transformer {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RetryTransformer.class);

	private static final String HEADER_KEY_ERROR = "ERROR";

	private static final String HEADER_KEY_ERROR_RETRY_COUNT = "ERROR_RETRY_COUNT";

	private List<RetryConfiguration> configurations;

	private Map<Class<? extends Exception>, RetryConfiguration> configurationsMap;

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void afterPropertiesSet() throws Exception {

		configurationsMap = new HashMap<Class<? extends Exception>, RetryConfiguration>();

		for (RetryConfiguration configuration : configurations) {
			Class<? extends Exception> exceptionClass = (Class<? extends Exception>) Class
					.forName(configuration.getClassName());
			configurationsMap.put(exceptionClass, configuration);

		}

	}

	/**
	 * 
	 */
	@Override
	public Message<?> transform(Message<?> message) {

		if (message.getHeaders().containsKey(HEADER_KEY_ERROR)) {

			if (message.getPayload() instanceof MessagingException) {

				MessagingException messagingException = (MessagingException) message
						.getPayload();

				RetryConfiguration configuration = configurationsMap
						.get(messagingException.getCause().getClass());

				if (configuration == null) {
					LOGGER.error("Unhandled error: "
							+ messagingException.getCause()
									.getLocalizedMessage());
					messagingException.getCause().printStackTrace();
				} else {
					message = checkErrorMessage(
							messagingException.getFailedMessage(),
							configuration);
				}
			} else if (message.getPayload() instanceof TransactionSystemException) {
				return null;
			}

		}

		return message;
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	private Message<?> checkErrorMessage(Message<?> failedMessage,
			RetryConfiguration configuration) {

		if (configuration.getRetries() == 0) {
			// TODO send the message to the error queue as defined on the
			// configuration
			return null;
		}

		Integer count = (Integer) failedMessage.getHeaders().get(
				HEADER_KEY_ERROR_RETRY_COUNT);

		if (count != null && count >= configuration.getRetries()) {
			// TODO send the message to the error queue as defined on the
			// configuration
			return null;
		} else if (count == null) {
			MessageBuilder<?> builder = MessageBuilder
					.withPayload(failedMessage.getPayload());
			builder.copyHeaders(failedMessage.getHeaders());
			Map<String, Object> newHeader = new HashMap<String, Object>();
			newHeader.put(HEADER_KEY_ERROR_RETRY_COUNT, new Integer(1));
			builder.copyHeadersIfAbsent(newHeader);
			failedMessage = builder.build();
		} else {
			failedMessage.getHeaders().put(HEADER_KEY_ERROR_RETRY_COUNT,
					count++);
		}

		return failedMessage;

	}

	public List<RetryConfiguration> getConfigurations() {
		return configurations;
	}

	public void setConfigurations(List<RetryConfiguration> configurations) {
		this.configurations = configurations;
	}

}
