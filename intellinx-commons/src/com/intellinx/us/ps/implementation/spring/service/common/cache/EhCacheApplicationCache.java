package com.intellinx.us.ps.implementation.spring.service.common.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class EhCacheApplicationCache<T> extends AbstractApplicationCache<T>
		implements IApplicationCache<T>, InitializingBean {

	private String region;

	private CacheManager cacheManager;

	private Cache cache;

	/**
	 * 
	 * @param parser
	 */
	public EhCacheApplicationCache(SpelExpressionParser parser) {
		super(parser);
	}

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		//
		Assert.notNull(region, "Region is required");

		// Start Cache Manager
		cacheManager = CacheManager.getInstance();
		cache = cacheManager.getCache(region);

	}

	/**
	 * 
	 */
	@Override
	public boolean isKeyInCache(Message<T> message) {
		return cache.isKeyInCache(keyValue(message));
	}

	/**
	 * 
	 */
	@Override
	public Object get(Message<T> message) {
		return cache.get(keyValue(message));
	}

	@Override
	public void put(Object value, Message<T> message) {
		Element element = new Element(keyValue(message), value);
		cache.put(element);
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

}
