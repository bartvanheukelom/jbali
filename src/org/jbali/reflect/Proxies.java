package org.jbali.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import com.google.common.base.Preconditions;

public class Proxies {
	
	/**
	 * Compared to InvocationHandler:
	 * <ul>
	 * <li>always returns <code>null</code></li>
	 * <li>proxy argument is ignored</li>
	 * <li>no args is an empty array instead of <code>null</code></li>
	 * </ul>
	 */
	public interface SimpleInvocationHandler {
		
		void invoke(Method method, Object[] args) throws Throwable;
		
		default InvocationHandler toInvocationHandler() {
			return (proxy, method, args) -> {
				invoke(method, args != null ? args : new Object[0]);
				return null;
			};
		}
		
	}
	
	public static final InvocationHandler MOCK_HANDLER = (proxy, method, args) -> null;
	
	public static <R> R create(Class<R> type, SimpleInvocationHandler handler) {
		return create(type, handler.toInvocationHandler());
	}

	public static <R> R create(Class<R> type, InvocationHandler handler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}
	
	public static <R> R createMock(Class<R> type) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, MOCK_HANDLER));
	}
	
	public static String invocationToString(Method method, Object[] args) {
		String argsStr = null;
		try {
			argsStr = (args == null) ? "" : Arrays.toString(args);
		} catch (Throwable e) {
			argsStr = "toString error " + e.getMessage();
		}
		return method.getName() + "(" + argsStr + ")";
	}

	
//	public static <R> R createForking(Class<R> type, SimpleInvocationHandler handler) {
//		return create(type, (proxy, method, args) -> {
//			ThreadPool.execute(() -> {
//				try {
//					handler.invoke(method, args);
//				} catch (Throwable e) {
//					throw Exceptions.wrap(e);
//				}
//			});
//			return null;
//		});
//	}
	
//	public static <R> R createDecorator(Class<R> type, R target, Runnable pre, Runnable post)
	
	/**
	 * Take the exception from a Method.invoke call and make it suitable for rethrowing from a proxy,
	 * making the presence of the proxy invisible. Meaning:<ul>
	 * <li>InvocationTargetException and ExceptionInInitializerError will return their cause.</li>
	 * <li>Other exceptions declared by invoke's doc, which should not occur if the proxy is implemented properly, are wrapped in {@link AssertionError}.</li>
	 * <li>Any other exception is returned as-is. This should usually only include the likes of {@link OutOfMemoryError}.</li>
	 */
	public static Throwable transformInvokeError(Throwable e) {
		if (e instanceof InvocationTargetException || e instanceof ExceptionInInitializerError) return e.getCause();
		if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException || e instanceof NullPointerException)
			return new AssertionError(e);
		return e;
	}
	
	/**
	 * Simple handling of the following Object methods:
	 * <ul>
	 * <li><i>toString</i>: returns <code>toStringed</code></li>
	 * <li><i>equals</i>:   returns <code>proxy</code> == args[0]</li>
	 * <li><i>hashCode</i>: return System.identityHashCode(proxy)</li>
	 * </ul>
	 * @return The value to return, or <code>null</code> if the method is not one of those listed above.
	 */
	public static Object handleObjectMethods(Object proxy, Method method, Object[] args, String toStringed) {
		Preconditions.checkNotNull(toStringed);
		if (method.equals(Methods.OBJECT_TOSTRING)) return toStringed;
		if (method.equals(Methods.OBJECT_EQUALS))   return proxy == args[0];
		if (method.equals(Methods.OBJECT_HASHCODE)) return System.identityHashCode(proxy);
		return null;
	}
	
}
