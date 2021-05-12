package org.jbali.service;

import kotlin.reflect.KType;
import org.jbali.coroutines.Blocking;
import org.jetbrains.annotations.Nullable;

/**
 * Take a request in the form of an operation name and input data, and see that it's properly processed.
 */
public interface ServiceHandler<C> {
	
	interface OperationHandler<C> {
		@Nullable
		@Blocking
		Object handle(C context, Object input);
		Class<?> getInputType();
		KType getReturnType();
	}

	OperationHandler<C> getOperation(String operation);
}
