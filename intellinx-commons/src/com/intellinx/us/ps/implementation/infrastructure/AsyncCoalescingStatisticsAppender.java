package com.intellinx.us.ps.implementation.infrastructure;

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;

/**
 * 
 * @author RenatoM
 * 
 */
public class AsyncCoalescingStatisticsAppender extends
		org.perf4j.log4j.AsyncCoalescingStatisticsAppender {

	/**
	 * 
	 * @param appenderName
	 */
	public void setAppenderNew(String appenderName) {

		Enumeration<?> enumeration = LogManager.getLogger(appenderName)
				.getAllAppenders();

		while (enumeration.hasMoreElements()) {
			Appender appender = (Appender) enumeration.nextElement();
			if (appender.getName().equals(appenderName))
				super.addAppender(appender);
		}

	}
}
