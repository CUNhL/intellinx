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
		super.afterPropertiesSet();

		//
		Assert.notNull(region, "Region is required");

		// Start Cache Manager
		cacheManager = CacheManager.getInstance();
		cache = cacheManager.getCache(region);

		Assert.notNull(cache,
				"Cache Name was not found on ehcache configuraiton file, please add");

	}

	/**
	 * 
	 */
	@Override
	public boolean isKeyInCache(Message<T> message) {
		Object key = keyValue(message);
		if (cache.isKeyInCache(key)) {
			Element element = cache.get(key);
			if (element != null) {
				return !cache.isExpired(element);
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * 
	 */
	@Override
	public Object get(Message<T> message) {
		Element element = cache.get(keyValue(message));
		return element.getObjectValue();
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
