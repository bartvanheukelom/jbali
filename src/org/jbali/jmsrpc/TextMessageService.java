package org.jbali.jmsrpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.jbali.json.JSONArray;
import org.jbali.reflect.Methods;
import org.jbali.serialize.JavaJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class TextMessageService {

	private static final Logger log = LoggerFactory.getLogger(TextMessageService.class);
	
	private final Object endpoint;
	private final ImmutableMap<String, Method> methods;
	
	public TextMessageService(Object endpoint) {
		this.endpoint = endpoint;
		methods = Methods.mapPublicMethodsByName(endpoint.getClass());
	}
	
	public String handleRequest(String request) {
		
		JSONArray response;
		String methName = "?";
		boolean requestLogged = false;
		
		try {
			
			// parse request json
			JSONArray reqJson;
			try {
				reqJson = new JSONArray(request);
			} catch (Throwable e) {
				throw new IllegalArgumentException("Could not parse request", e);
			}
			
			// determine method
			methName = reqJson.getString(0);
			Method method = methods.get(methName);
			if (method == null) throw new NoSuchElementException("Unknown method " + methName);
			
			// read arguments
			Parameter[] pars = method.getParameters();
			Object[] args = new Object[pars.length];
			for (int p = 0; p < pars.length; p++) {
				int ri = p+1;
				Object arg;
				if (reqJson.length() < ri+1) {
					if (!requestLogged) {
						log.info(methName + ":");
						requestLogged = true;
					}
					log.info("- Arg #" + p + " omitted");
//					arg = pars[p].getType() == Maybe.class ? Maybe.unknown() : null;
					arg = null;
				} else {
					arg = JavaJsonSerializer.unserialize(reqJson.get(ri));
				}
				args[p] = arg;
			}
			int extraArgs = reqJson.length() - 1 - pars.length;
			if (extraArgs > 0) {
				if (!requestLogged) {
					log.info(methName + ":");
					requestLogged = true;
				}
				log.info("- " + extraArgs + " args too many ignored.");
			}
			
			// execute
			Object ret;
			try {
				ret = method.invoke(endpoint, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
			// TODO return more info for mismatched arguments, such as index
			
			// return response
			response = JSONArray.create(1, JavaJsonSerializer.serialize(ret));
			
		} catch (Throwable e) {
			
			log.warn("Error in text request for method " + methName, e);
			
			StackTraceElement[] orgTrace = e.getStackTrace();
			StackTraceElement[] locTrace = new Throwable().getStackTrace();
			e.setStackTrace(Arrays.copyOfRange(orgTrace, 0, orgTrace.length - locTrace.length));
			
			try {
				response = JSONArray.create(0, JavaJsonSerializer.serialize(e));
			} catch (Throwable e2) {
				log.warn("Error serializing error", e2);
				response = JSONArray.create(0, JavaJsonSerializer.serialize(new RuntimeException("Error occurred but could not be serialized")));
			} finally {
				e.setStackTrace(orgTrace);
			}
		}
		
		try {
			return response.toString(2);
		} catch (Throwable e) {
			log.warn("Error toStringing JSON response", e);
			return "[0, null]";
		}
		
	}
	
}
