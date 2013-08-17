package com.intellinx.us.ps.implementation.spring.service.util;

import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;

import com.intellinx.integration.bom.mapper.MapEntityMapper;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class EntityManagerWrapper implements Transformer {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(EntityManagerWrapper.class);

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private MapEntityMapper entityMapperParser;

	/**
	 * 
	 */
	@Override
	public Message<?> transform(Message<?> message) {

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch("DroolsEntryPointService",
					"EntityManagerWrapper", LOGGER_PERFORMANCE);

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(entityMapperParser.getClass().getName() + ","
					+ entityMapperParser.getClass().getPackage().getName());

		try {
			Object payload = entityMapperParser.doMap(message.getPayload());
			MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
			builder.copyHeaders(message.getHeaders());
			message = builder.build();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop("EntityManagerWrapper", "Done");

		return message;
	}

	public MapEntityMapper getEntityMapperParser() {
		return entityMapperParser;
	}

	public void setEntityMapperParser(MapEntityMapper entityMapperParser) {
		this.entityMapperParser = entityMapperParser;
	}

}
