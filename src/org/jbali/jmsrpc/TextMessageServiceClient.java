package org.jbali.jmsrpc;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Function;

import org.jbali.json.JSONArray;
import org.jbali.serialize.JavaJsonSerializer;

public class TextMessageServiceClient {

	public static <S> S create(Class<S> iface, Function<String, String> requestHandler) {
		
		return iface.cast(Proxy.newProxyInstance(
			iface.getClassLoader(), new Class<?>[]{iface},
			(proxy, method, args) -> {
				
				JSONArray reqJson = JSONArray.create(method.getName());
				if (args != null) Arrays.stream(args)
					.map(JavaJsonSerializer::serialize)
					.forEach(reqJson::put);
				
				String respStr = requestHandler.apply(reqJson.toString(2));
				JSONArray respJson = new JSONArray(respStr);
				Object respOrError = JavaJsonSerializer.unserialize(respJson.get(1));
				
				if (respJson.getInt(0) == 0) {
					
					// the response should be an exception
					if (respOrError == null) throw new IllegalStateException("Service returned a null error");
					Throwable err;
					try {
						err = (Throwable) respOrError;
					} catch (ClassCastException e) {
						throw new IllegalStateException("Service returned an error that is not Throwable but " + respOrError.getClass());
					}
					
					// merge the remote and local stack traces
					
					StackTraceElement[] remTrace = err.getStackTrace();
					StackTraceElement[] locTrace = new Throwable().getStackTrace();
					
					StackTraceElement[] st = new StackTraceElement[remTrace.length + 1 + locTrace.length];
					System.arraycopy(remTrace, 0, st, 0, remTrace.length);
					st[remTrace.length] = new StackTraceElement("=============", "", "TextMessageService", -3);
					System.arraycopy(locTrace, 0, st, remTrace.length+1, locTrace.length);
					
					err.setStackTrace(st);
					
					// throw the remote error
					throw err;
				}
				
				return respOrError;
				
			}
		));
		
	}
	
}
