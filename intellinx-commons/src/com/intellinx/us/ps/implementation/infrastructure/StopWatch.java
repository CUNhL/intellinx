package com.intellinx.us.ps.implementation.infrastructure;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class StopWatch extends org.springframework.util.StopWatch {

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	// ------------------------------ FIELDS ------------------------------

	org.springframework.util.StopWatch.TaskInfo m_minimumTimeTask = null;
	org.springframework.util.StopWatch.TaskInfo m_maximumTimeTask = null;
	private final String id;

	// --------------------------- CONSTRUCTORS ---------------------------

	public StopWatch() {
		this("");
	}

	public StopWatch(String id) {
		super(id);
		this.id = id;
	}

	@Override
	public void setKeepTaskList(boolean keepTaskList) {
		throw new UnsupportedOperationException(
				"The task list is always kept to be able to calculate the min, max and average");
	}

	// -------------------------- PUBLIC METHODS --------------------------

	public long getMinimumTimeMillis() {
		if (m_minimumTimeTask != null) {
			return m_minimumTimeTask.getTimeMillis();
		} else {
			return -1;
		}
	}

	public long getMaximumTimeMillis() {
		if (m_maximumTimeTask != null) {
			return m_maximumTimeTask.getTimeMillis();
		} else {
			return -1;
		}
	}

	public void stop() throws IllegalStateException {
		super.stop();
		updateMinimumTime();
		updateMaximumTime();
	}

	public String shortSummary() {
		StringBuilder builder = new StringBuilder();
		builder.append("StopWatch '").append(id)
				.append("': running time (millis) = ")
				.append(getTotalTimeMillis());

		if (getTaskCount() > 0) {
			builder.append(LINE_SEPARATOR)
					.append("-----------------------------------------")
					.append(LINE_SEPARATOR);
			builder.append("min: ").append(m_minimumTimeTask.getTimeMillis())
					.append(" ms (").append(m_minimumTimeTask.getTaskName())
					.append(")").append(LINE_SEPARATOR);
			builder.append("max: ").append(m_maximumTimeTask.getTimeMillis())
					.append(" ms (").append(m_maximumTimeTask.getTaskName())
					.append(")").append(LINE_SEPARATOR);
			builder.append("avg: ").append(getAverageTimeMillis())
					.append(" ms");
		}
		return builder.toString();
	}

	// -------------------------- PRIVATE METHODS --------------------------

	private void updateMinimumTime() {
		if (m_minimumTimeTask == null) {
			m_minimumTimeTask = getLastTaskInfo();
		} else {
			if (getLastTaskTimeMillis() < m_minimumTimeTask.getTimeMillis()) {
				m_minimumTimeTask = getLastTaskInfo();
			}
		}
	}

	private void updateMaximumTime() {
		if (m_maximumTimeTask == null) {
			m_maximumTimeTask = getLastTaskInfo();
		} else {
			if (getLastTaskTimeMillis() > m_maximumTimeTask.getTimeMillis()) {
				m_maximumTimeTask = getLastTaskInfo();
			}
		}
	}

	public long getAverageTimeMillis() {
		if (getTaskCount() > 0) {
			return getTotalTimeMillis() / getTaskCount();
		} else {
			return -1L;
		}
	}
}