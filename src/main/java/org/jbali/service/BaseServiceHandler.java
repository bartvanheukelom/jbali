package org.jbali.service;

import com.google.common.collect.ImmutableMap;

import java.util.NoSuchElementException;

/**
 * Abstract base class for generated endpoint-based {@link ServiceHandler}s.
 */
public abstract class BaseServiceHandler<C> implements ServiceHandler<C> {

	private final ImmutableMap<String, OperationHandler<C>> handlers;
	
	protected BaseServiceHandler(ImmutableMap<String, OperationHandler<C>> handlers) {
		this.handlers = handlers;
	}

	public OperationHandler<C> getOperation(String operation) {
		final OperationHandler<C> op = handlers.get(operation);
		if (op == null) {
			throw new NoSuchElementException("Unknown operation " + operation);
		}
		return op;
	}
	
}
