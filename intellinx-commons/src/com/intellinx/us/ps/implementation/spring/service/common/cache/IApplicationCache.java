package com.intellinx.us.ps.implementation.spring.service.common.cache;

import org.springframework.integration.Message;

/**
 * 
 * @author Renato Mendes
 * 
 * @param <T>
 */
public interface IApplicationCache<T> {

	public void put(Object value, Message<T> message);

	public boolean isKeyInCache(Message<T> message);

	public Object get(Message<T> message);

}
