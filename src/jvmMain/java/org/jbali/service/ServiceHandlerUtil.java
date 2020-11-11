package org.jbali.service;

import com.google.common.collect.ImmutableMap;
import org.jbali.service.ServiceHandler.OperationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ServiceHandlerUtil {
	private final static Logger log = LoggerFactory.getLogger(ServiceHandlerUtil.class);
	/**
	 * Create a map of callable methods on a service interface
	 * 
	 * super(ServiceHandlerUtil.createCallableMethodsMap(impl));
	 * 
	 * @param impl
	 * @return
	 */
	public static <C> ImmutableMap<String, OperationHandler<C>> createCallableMethodsMap(Object impl) {
		Class<?> clazz = impl.getClass();
		Map<String, OperationHandler<C>> handlers = new HashMap<>();
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {			
			if (method.getName().contains("lambda"))
				continue;
			if (method.getDeclaringClass() == Object.class)
				continue;
			AutomaticOperationHandler<C> operationHandler = new AutomaticOperationHandler<C>(clazz, impl, method);
			handlers.put(method.getName(), operationHandler);
		}
		return ImmutableMap.copyOf(handlers);		
	}	
}
