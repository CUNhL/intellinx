package com.intellinx.us.ps.implementation.spring.namespace;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.intellinx.us.ps.implementation.spring.service.persistence.lookup.LookupService;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class LookupBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * 
	 */
	@Override
	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {

		BeanDefinitionBuilder lookupBeanBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(LookupService.class);

		return lookupBeanBuilder.getBeanDefinition();

	}

}
