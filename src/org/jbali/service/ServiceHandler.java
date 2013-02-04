package org.jbali.service;

/**
 * Take a request in the form of an operation name and input data, and see that it's properly processed.
 */
public interface ServiceHandler {

	public Class<?> getInputType(String operation);
	
	public Object handle(String operation, Object input);
	
}
