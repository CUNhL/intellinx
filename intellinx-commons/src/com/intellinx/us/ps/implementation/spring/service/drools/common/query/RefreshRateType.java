package com.intellinx.us.ps.implementation.spring.service.drools.common.query;

import java.util.Calendar;

/**
 * 
 * @author RenatoM
 * 
 */
public enum RefreshRateType {

	SECONDS, MINUTES, HOURS, DAYS;

	public int toCalendar() {
		switch (this) {
		case DAYS:
			return Calendar.DAY_OF_YEAR;
		case HOURS:
			return Calendar.HOUR;
		case MINUTES:
			return Calendar.MINUTE;
		case SECONDS:
			return Calendar.SECOND;
		default:
			return 0;
		}
	}

}
