package org.jbali.reflect;

import com.google.common.base.Preconditions;
import org.jbali.serialize.JavaSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

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
		
		void invoke(@NotNull Method method, Object @NotNull [] args) throws Throwable;

		default void invokeNullableArgs(@NotNull Method method, Object @Nullable [] args) throws Throwable {
			invoke(method, args == null ? new Object[0] : args);
		}
		
		default InvocationHandler toInvocationHandler() {
			return (proxy, method, args) -> {
				invokeNullableArgs(method, args);
				return null;
			};
		}
		
	}
	
	public static final InvocationHandler MOCK_HANDLER = (proxy, method, args) -> {
		Class<?> rt = method.getReturnType();
		if (!rt.isPrimitive() || rt == void.class) return null;
		else {
			if (rt == char.class)    return '\0';
			if (rt == byte.class)    return 0;
			if (rt == short.class)   return 0;
			if (rt == int.class)     return 0;
			if (rt == long.class)    return 0L;
			if (rt == float.class)   return 0.0f;
			if (rt == double.class)  return 0.0;
			if (rt == boolean.class) return false;
			return null; // should not get here
		}
	};

	public static <R> R createSimple(Class<R> type, SimpleInvocationHandler handler) {
		return create(type, handler);
	}

	public static <R> R create(Class<R> type, SimpleInvocationHandler handler) {
		return create(type, handler.toInvocationHandler());
	}

	public static <R> R create(Class<R> type, InvocationHandler handler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}
	
	public static <R> R createMock(Class<R> type) {
		return createMock(type, null);
	}

	public static <R> R createMock(Class<R> type, String printName) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, printName == null ? MOCK_HANDLER : (proxy, method, args) -> {
			System.out.println(printName + ": " + invocationToString(method, args));
			return MOCK_HANDLER.invoke(proxy, method, args);
		}));
	}

	public static <R> R createThrowingMock(Class<R> type) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
			throw new RuntimeException("Fake exception for testing: " + invocationToString(method, args));
		}));
	}

	// NOTE new proxy types are preferably not added here

	/**
	 * Copies all arguments using Java serialization before passing invocations to `real`.
	 * Useful for e.g. testing remote interfaces that use this serialization.
	 */
	public static <R> R createSerializing(Class<R> type, R real) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					args[i] = JavaSerializer.copy((Serializable) args[i]);
				}
			}
			return invokeTransparent(method, real, args);
		}));
	}
	
	public static @NotNull String invocationToString(Method method, Object[] args) {
		String argsStr = null;
		try {
			argsStr = (args == null) ? "" : Arrays.toString(args);
		} catch (Throwable e) {
			argsStr = "toString error " + e.getMessage();
		}
		return method.getName() + "(" + argsStr + ")";
	}

	/**
	 * Invoke method on obj, and if it throws an exception, throw that without the surrounding InvocationTargetException.
	 */
	public static Object invokeTransparent(Method method, Object obj, Object[] args) throws Throwable {
		try {
			return method.invoke(obj, args);
		} catch (InvocationTargetException e) {
			// TODO may also strip stack trace
			throw e.getCause();
		}
	}

	/**
	 * Take the exception from a Method.invoke call and make it suitable for rethrowing from a proxy,
	 * making the presence of the proxy invisible. Meaning:<ul>
	 * <li>InvocationTargetException and ExceptionInInitializerError will return their cause.</li>
	 * <li>Other exceptions declared by invoke's doc, which should not occur if the proxy is implemented properly, are wrapped in {@link AssertionError}.</li>
	 * <li>Any other exception is returned as-is. This should usually only include the likes of {@link OutOfMemoryError}.</li>
	 */
	public static Throwable transformInvokeError(Throwable e) {
		if (e instanceof InvocationTargetException) return e.getCause();
		if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException || e instanceof NullPointerException)
			return new AssertionError(e);
		return e;
	}
	
	/**
	 * Simple handling of the following Object methods:
	 * <ul>
	 * <li><i>toString</i>: returns <code>toStringed</code></li>
	 * <li><i>equals(x)</i>:   returns <code>thiz</code> == x</li>
	 * <li><i>hashCode</i>: return System.identityHashCode(thiz)</li>
	 * </ul>
	 * @return The value to return, or <code>null</code> if the method is not one of those listed above.
	 */
	public static Object handleTEH(Object thiz, Method method, Object[] args, String toStringed) {
		Preconditions.checkNotNull(toStringed);
		if (method.equals(Methods.OBJECT_TOSTRING)) return toStringed;
		if (method.equals(Methods.OBJECT_EQUALS))   return thiz == args[0];
		if (method.equals(Methods.OBJECT_HASHCODE)) return System.identityHashCode(thiz);
		return null;
	}
	
}
