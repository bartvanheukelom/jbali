package org.jbali.service;

/**
 * Take a request in the form of an operation name and input data, and see that it's properly processed.
 */
public interface ServiceHandler<C> {
	
	interface OperationHandler<C> {
		Object handle(C context, Object input);
		Class<?> getInputType();
	}

	OperationHandler<C> getOperation(String operation);
}
