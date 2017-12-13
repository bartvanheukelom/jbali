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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class TextMessageService {
	
	public static final int RQIDX_METHOD = 0;
	
	public static final int RSIDX_STATUS = 0;
	public static final int RSIDX_RESPONSE = 1;
	
	public static final int STATUS_OK = 1;
	public static final int STATUS_ERROR = 0;

	private static final Logger log = LoggerFactory.getLogger(TextMessageService.class);
	
	private final Object endpoint;
	private final ImmutableMap<String, Method> methods;
	
	public TextMessageService(Object endpoint) {
		Preconditions.checkNotNull(endpoint);
		this.endpoint = endpoint;
		methods = Methods.mapPublicMethodsByName(endpoint.getClass());
	}
	
	public String handleRequest(String request) {
		
		JSONArray response;
		String methName = "?";
		String className = endpoint.getClass().getName();
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
			methName = reqJson.getString(RQIDX_METHOD);
			Method method = methods.get(methName);
			if (method == null) throw new NoSuchElementException("Unknown method '" + methName + "'");
			if (!className.equals(method.getDeclaringClass().getName()))
				className += ">" + method.getDeclaringClass().getName();
			
			// read arguments
			Parameter[] pars = method.getParameters();
			Object[] args = new Object[pars.length];
			for (int p = 0; p < pars.length; p++) {
				Parameter par = pars[p];
				int indexInReq = p+1;
				Object arg;
				if (reqJson.length() < indexInReq+1) {
					// this parameter has no argument
					if (!requestLogged) {
						log.info(methName + ":");
						requestLogged = true;
					}
					log.info("- Arg #" + p + " (" + par.getType() + " " + par.getName() + ") omitted");
					arg = null; // let's hope that's sufficient
				} else {
					arg = JavaJsonSerializer.unserialize(reqJson.get(indexInReq));
				}
				args[p] = arg;
			}
			// check for more args than used
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
			} catch (InvocationTargetException | ExceptionInInitializerError e) {
				// InvocationTargetException: actual exception inside method.
				// ExceptionInInitializerError: always unchecked (initializers can't throw checked).
				throw e.getCause();
			} catch (IllegalAccessException | NullPointerException e) {
				// IllegalAccessException: method is public, should not happen.
				// NullPointerException: endpoint is not null, should not happen.
				throw new RuntimeException("TextMessageService internal error", e);
			}
			
			// return response
			response = JSONArray.create(STATUS_OK, JavaJsonSerializer.serialize(ret));
			
		} catch (Throwable e) {

			// remove the current stack trace from the error stacktraces,
			// because it's not relevant to the client or logs.
			// TODO remove up to the endpoint as well, e.g.:
			//			at com.blabla.DataServer$RemoteDataServerImpl.authByXId(DataServer.java:123) ~[dataserver.jar:na]
			//			...
			//			at org.jbali.jmsrpc.TextMessageService.handleRequest(TextMessageService.java:95) ~[bali.jar:na]
			StackTraceElement[] locTrace = Thread.currentThread().getStackTrace();
			if (locTrace.length >= 3) { // you never know on some VMs
				StackTraceElement caller = locTrace[2];
				StackTraceElement thisMethod = locTrace[1];

				Throwable toClean = e;
				while (toClean != null) {
					StackTraceElement[] errTrace = toClean.getStackTrace();

					// find the calling method in the exception stack
					for (int i = errTrace.length - 1; i >= 1; i--) {
						if (errTrace[i].equals(caller)) {
							// check if the calling method did in fact call this method
							// (line number won't match)
							StackTraceElement nextCall = errTrace[i - 1];
							if (nextCall.getClassName().equals(thisMethod.getClassName())
									&& nextCall.getMethodName().equals(thisMethod.getMethodName())) {
								// snip!
								toClean.setStackTrace(Arrays.copyOfRange(errTrace, 0, i));
								break;
							}
						}
					}
					toClean = toClean.getCause();
				}
			}

			log.warn("Error in text request for method " + className + "." + methName, e);
			
			try {
				response = JSONArray.create(STATUS_ERROR, JavaJsonSerializer.serialize(e));
			} catch (Throwable serEr) {
				log.warn("Error while serializing error", serEr);
				response = JSONArray.create(STATUS_ERROR, JavaJsonSerializer.serialize(new RuntimeException("Error occurred but could not be serialized")));
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
