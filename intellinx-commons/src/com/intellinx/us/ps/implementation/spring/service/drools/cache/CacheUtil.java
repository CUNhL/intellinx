package com.intellinx.us.ps.implementation.spring.service.drools.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class CacheUtil {

	private String cacheName;

	/**
	 * 
	 */
	public void put(String key, Object information) {

		CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache(cacheName);

		//
		Element element = cache.get(key.toString());

		element = new Element(key, information);
		cache.put(element);

	}

	public String getCacheName() {
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

}
