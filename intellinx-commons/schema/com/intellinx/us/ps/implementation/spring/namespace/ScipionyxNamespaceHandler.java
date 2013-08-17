package com.intellinx.us.ps.implementation.spring.namespace;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class ScipionyxNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("lookup", new LookupBeanDefinitionParser());
	}

}
