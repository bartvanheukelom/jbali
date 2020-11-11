package org.jbali.service;

import org.jbali.errors.ExceptionToolsKt;
import org.jbali.service.ServiceHandler.OperationHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class AutomaticOperationHandler<C> implements OperationHandler<C> {
	private Class<?> clazz;
	private Object impl;
	private Method method;
	private Class<?> inputType;
	private Class<?> returnType;

	public AutomaticOperationHandler(Class<?> clazz, Object impl, Method method) {
		this.clazz = clazz;
		this.impl = impl;			
		this.method = method;
		validate();
		if (method.getParameterTypes().length == 2)
			this.inputType = method.getParameterTypes()[1];
		else
			this.inputType = Void.TYPE;
		this.returnType = method.getReturnType();
	}

	private void validate() {
		assert(method.getParameterTypes().length <= 1);
		assert(clazz.isInstance(impl));
	}

	@Override
	public Object handle(C context, Object input) {
		try {
			Object answer;
			if (method.getParameterTypes().length == 2)
				answer = method.invoke(impl, context, input);
			else
				answer = method.invoke(impl, context);
			if (returnType.equals(Void.TYPE))
				return null;
			return answer;				
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw ExceptionToolsKt.ensureUnchecked(e.getCause());
		}
	}

	@Override
	public Class<?> getInputType() {
		if (inputType.equals(Void.TYPE))
			return null;
		return inputType;
	}

	@Override
	public String toString() {
		return method.getName();
	}	
}
