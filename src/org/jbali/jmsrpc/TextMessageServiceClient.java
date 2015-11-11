package org.jbali.jmsrpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jbali.json.JSONArray;
import org.jbali.reflect.Proxies;
import org.jbali.serialize.JavaJsonSerializer;

public class TextMessageServiceClient {
	
	private static class RethrowException extends Exception {
		public RethrowException(Throwable cause) {
			super(cause);
		}
	}

	/**
	 * Create an implementation of the given interface that will:
	 * <ul>
	 * <li>Serialize invocations of its methods<sup>1</sup> to JSON using {@link JavaJsonSerializer}.</li>
	 * <li>Pass that JSON to the given <code>requestHandler</code>, which is presumed to send it to some (remote) service, and return the string response.</li>
	 * <li>Parse the string response and return it, or if the response is an exception, throw it.</li>
	 * </ul>
	 * <sup>1</sup>: The following methods are executed locally, so not passed to <code>requestHandler</code>:
	 * <ul>
	 * <li><i>toString</i>: returns "TextMessageServiceClient[" + iface.getSimpleName() + "]"</li>
	 * <li><i>equals</i>: Uses ==</li>
	 * <li><i>hashCode</i>: Uses System.identityHashCode()</li>
	 * </ul>
	 */
	public static <S> S create(Class<S> iface, Function<String, String> requestHandler) {
		
		final String toStringed = "TextMessageServiceClient[" + iface.getSimpleName() + "]";
		
		return Proxies.create(iface, (proxy, method, args) -> {
			
			try {

				// catch local methods
				final Object omRes = Proxies.handleObjectMethods(proxy, method, args, toStringed);
				if (omRes != null) return omRes;
				
				
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
				Object respOrError = JavaJsonSerializer.unserialize(respJson.get(TextMessageService.RSIDX_RESPONSE));
				
				// check for error
				if (respJson.getInt(TextMessageService.RSIDX_STATUS) == TextMessageService.STATUS_OK) return respOrError;
				
				
				// --- an error was returned --- //
					
				// the response should be an exception
				Throwable err = makeSureIsThrowable(respOrError);
				
				// add the local stack trace to the remote exception,
				// otherwise that info is lost - unless we wrap the exception in a new local one,
				// which we don't want because it breaks the remote API
				augmentStackTrace(err, 1); // discard invocation handler from trace
				
				throw new RethrowException(err);
				
			} catch (RethrowException e) {
				throw e.getCause();
			} catch (Throwable e) {
				throw new TextMessageServiceClientException(
						"A local/meta exception occured when invoking " + toStringed + "." + method.getName() + ": " + e, e);
			}
			
		});
		
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
