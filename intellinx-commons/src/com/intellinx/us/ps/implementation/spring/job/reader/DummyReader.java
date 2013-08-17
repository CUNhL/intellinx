package com.intellinx.us.ps.implementation.spring.job.reader;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * 
 * @author RenatoM
 *
 * @param <T>
 */
public class DummyReader<T> implements ItemReader<T> {
	
	/**
	 * 
	 */
	public T read() throws Exception, UnexpectedInputException, ParseException,
			NonTransientResourceException {
		return null;
	}

}
