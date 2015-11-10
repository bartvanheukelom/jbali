package org.jbali.jmsrpc;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jbali.json.JSONArray;
import org.jbali.serialize.JavaJsonSerializer;

public class TextMessageServiceClient {

	/**
	 * Create an implementation of the given interface that will:
	 * <ul>
	 * <li>Serialize invocations of its methods to JSON using {@link JavaJsonSerializer}.</li>
	 * <li>Pass that JSON to the given <code>requestHandler</code>, which is presumed to send it to some (remote) service, and return the string response.</li>
	 * <li>Parse the string response and return it, or if the response is an exception, throw it.</li>
	 * </ul>
	 */
	public static <S> S create(Class<S> iface, Function<String, String> requestHandler) {
		
		return iface.cast(Proxy.newProxyInstance(
			iface.getClassLoader(), new Class<?>[]{iface},
			(proxy, method, args) -> {
				
				// don't need to log, or invoke the remote, for toString
				if (method.getName().equals("toString") && args == null)
					return "TextMessageServiceClient[" + iface.getSimpleName() + "]";
				
				// hashCode and equals (wait, notify, etc are not included in the proxied methods)
				if (method.getDeclaringClass() == Object.class)
					throw new IllegalArgumentException("Cannot call Object's methods - except toString - on a TextMessageServiceClient");
				
				
				// --- ok, it's a real method --- //
				
				// serialize the invocation to JSON
				JSONArray reqJson = JSONArray.create(method.getName());
				if (args != null) Arrays.stream(args)
					.map(JavaJsonSerializer::serialize)
					.forEach(reqJson::put);
				
				// send the request
				String respStr = requestHandler.apply(reqJson.toString(2));
				
				// parse the response
				JSONArray respJson = new JSONArray(respStr);
				Object respOrError = JavaJsonSerializer.unserialize(respJson.get(1));
				
				// check for error
				if (respJson.getInt(0) == 1) return respOrError;
				
				
				// --- an error occurred --- //
					
				// the response should be an exception
				Throwable err = makeSureIsThrowable(respOrError);
				
				// add the local stack trace to the remote exception,
				// otherwise that info is lost - unless we wrap the exception in a new local one,
				// which we don't want because it breaks the remote API
				augmentStackTrace(err, 1);
				
				// throw it over the fence
				throw err;
				
			}
		));
		
	}

	private static Throwable makeSureIsThrowable(Object err) {
		if (err == null) throw new IllegalStateException("Service returned a null error");
		try {
			return (Throwable) err;
		} catch (ClassCastException e) {
			throw new IllegalStateException("Service returned an error that is not Throwable but " + err.getClass());
		}
	}

	/**
	 * Append the current stack trace to the trace of the given exception.
	 * @param discard The number of elements to discard
	 */
	private static void augmentStackTrace(Throwable err, int discard) {
		
		List<StackTraceElement> remTrace = Arrays.asList(err.getStackTrace());
		List<StackTraceElement> locTrace = Arrays.asList(new Throwable().getStackTrace());
		
		// first the remote trace
		List<StackTraceElement> st = new ArrayList<>(remTrace);
		// then a separator
		st.add(new StackTraceElement("==========================", "", "TextMessageService", -3));
		// then part of the local trace
		st.addAll(locTrace.subList(discard+1, locTrace.size())); // +1 is augmentStackTrace
		
		err.setStackTrace(st.toArray(new StackTraceElement[st.size()]));
		
	}
	
}
