package com.intellinx.us.ps.implementation.spring.service.util;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.transformer.Transformer;

public class PerfornanceService implements Transformer, InitializingBean {

	private Map<Integer, Long> mapMin;

	private Map<Integer, Long> mapHour;

	private Map<Integer, Long> mapDay;

	private boolean enabled;

	@Override
	public void afterPropertiesSet() throws Exception {
		mapMin = Collections.synchronizedMap(new HashMap<Integer, Long>());
		mapDay = Collections.synchronizedMap(new HashMap<Integer, Long>());
		mapHour = Collections.synchronizedMap(new HashMap<Integer, Long>());
	}

	private Long lastHour = 0L;

	private Long maxLastHour = 0L;

	private Long lastMinute = 0L;

	private Long maxPerMinute = 0L;

	private Long lastDay = 0L;

	private Calendar lastRead;

	private Calendar lastReset;

	/**
	 * 
	 */
	@Override
	@ServiceActivator
	public Message<?> transform(Message<?> message) {

		if (isEnabled()) {

			synchronized (mapMin) {

				Integer min = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
						* 100 + Calendar.getInstance().get(Calendar.MINUTE);
				// Min
				if (mapMin.containsKey(min)) {
					Long counter = mapMin.get(min);
					counter++;
					mapMin.put(min, counter);
				} else {
					if (!mapMin.entrySet().isEmpty()) {
						lastMinute = mapMin.entrySet().iterator().next()
								.getValue();
						if (lastMinute > maxPerMinute)
							maxPerMinute = lastMinute;
					}
					mapMin.clear();
					mapMin.put(min, 1L);
					lastRead = Calendar.getInstance();
				}
			}

			synchronized (mapHour) {
				Integer hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				if (mapHour.containsKey(hour)) {
					Long counter = mapHour.get(hour);
					counter++;
					mapHour.put(hour, counter);
				} else {
					if (!mapHour.entrySet().isEmpty()) {
						lastHour = mapHour.entrySet().iterator().next()
								.getValue();
						if (lastHour > maxLastHour)
							maxLastHour = lastHour;
					}
					mapHour.clear();
					mapHour.put(hour, 1L);
					lastRead = Calendar.getInstance();
				}
			}

			synchronized (mapDay) {
				Integer day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
				if (mapDay.containsKey(day)) {
					Long counter = mapDay.entrySet().iterator().next()
							.getValue();
					counter++;
					mapDay.put(day, counter);
				} else {
					if (!mapDay.entrySet().isEmpty()) {
						lastDay = mapDay.get(day);
					}
					mapDay.clear();
					mapDay.put(day, 1L);
					lastRead = Calendar.getInstance();
				}
			}

		}

		return message;
	}

	/**
	 * 
	 */
	public void reset() {
		synchronized (mapMin) {
			mapMin.clear();
		}
		synchronized (mapHour) {
			mapHour.clear();
		}
		synchronized (mapDay) {
			mapDay.clear();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Long getLastHour() {
		return lastHour;
	}

	public Long getLastMinute() {
		return lastMinute;
	}

	public Long getLastDay() {
		return lastDay;
	}

	public Long getMaxLastHour() {
		return maxLastHour;
	}

	public Long getMaxPerMinute() {
		return maxPerMinute;
	}

	public Calendar getLastRead() {
		return lastRead;
	}

	public void setLastRead(Calendar lastRead) {
		this.lastRead = lastRead;
	}

	public Calendar getLastReset() {
		return lastReset;
	}

	public void setLastReset(Calendar lastReset) {
		this.lastReset = lastReset;
	}

}
