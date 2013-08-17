package com.intellinx.us.ps.implementation.spring.common.executor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class MultiThreadRejectExecutionHandler implements RejectedExecutionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadRejectExecutionHandler.class);

	/**
	 * 
	 */
	@Override
	public void rejectedExecution(Runnable runnable,
			ThreadPoolExecutor threadPoolExecutor) {

		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("rejectedExecution");

		}

	}

}
