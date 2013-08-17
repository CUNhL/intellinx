package com.intellinx.us.ps.implementation.spring.service.util;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class RetryConfiguration {

	private String className;

	private int retries;

	private int delay;

	private boolean throwError;

	private String errorQueueName;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public boolean isThrowError() {
		return throwError;
	}

	public void setThrowError(boolean throwError) {
		this.throwError = throwError;
	}

	public String getErrorQueueName() {
		return errorQueueName;
	}

	public void setErrorQueueName(String errorQueueName) {
		this.errorQueueName = errorQueueName;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

}
