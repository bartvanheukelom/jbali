package org.jbali.service;

import java.util.NoSuchElementException;

import com.google.common.collect.ImmutableMap;

/**
 * Abstract base class for generated endpoint-based {@link ServiceHandler}s.
 */
public abstract class BaseServiceHandler implements ServiceHandler {

	private final ImmutableMap<String, OperationHandler> handlers;
	
	protected BaseServiceHandler(ImmutableMap<String, OperationHandler> handlers) {
		this.handlers = handlers;
	}

	public OperationHandler getOperation(String operation) {
		final OperationHandler op = handlers.get(operation);
		if (op == null) {
			throw new NoSuchElementException("Unknown operation " + operation);
		}
		return op;
	}
	
}
