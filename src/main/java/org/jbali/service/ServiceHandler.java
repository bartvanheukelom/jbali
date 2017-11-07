package org.jbali.service;

/**
 * Take a request in the form of an operation name and input data, and see that it's properly processed.
 */
public interface ServiceHandler {
	
	public interface OperationHandler {
		Object handle(Object impl);
		Class<?> getInputType();
	}

	OperationHandler getOperation(String operation);
}
