package org.jbali.reflect;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

public class Methods {
	
	/** Object.toString() */
	public static final Method OBJECT_TOSTRING;
	/** Object.equals(Object obj) */
	public static final Method OBJECT_EQUALS;
	/** Object.hashCode() */
	public static final Method OBJECT_HASHCODE;
	static {
		try {
			OBJECT_TOSTRING = Object.class.getMethod("toString");
			OBJECT_EQUALS = Object.class.getMethod("equals", new Class<?>[]{Object.class});
			OBJECT_HASHCODE = Object.class.getMethod("hashCode");
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}
	
	private static final LoadingCache<Class<?>, ImmutableMap<String, Method>> classMethodCache =
			CacheBuilder.newBuilder()
				.weakKeys()
				.build(CacheLoader.from(Methods::mapClassMethods));

	public static boolean isPublic(Method m) {
		return Modifier.isPublic(m.getModifiers());
	}
	public static boolean isStatic(Method m) {
		return Modifier.isStatic(m.getModifiers());
	}
	
	/**
	 * Return wheter two methods have the same name. Currently only accepts methods without parameters.
	 * TODO compare parameters
	 */
	public static boolean equivalent(Method a, Method b) {
		Preconditions.checkArgument(a.getParameterCount() == 0);
		Preconditions.checkArgument(b.getParameterCount() == 0);
		return a.getName().equals(b.getName());
	}
	
	public static ImmutableMap<String, Method> mapPublicMethodsByName(Class<?> c) {
		return classMethodCache.getUnchecked(c);
	}
	
	private static ImmutableMap<String, Method> mapClassMethods(Class<?> c) {
		return mapMethodsByName(c.getMethods());
	}

	public static ImmutableMap<String, Method> mapMethodsByName(Method[] methods) {
		Map<String, Method> map = Maps.newHashMap();
		for (Method m : methods) {
			if (m.getDeclaringClass() == Object.class) continue;
			if (isStatic(m)) continue;
			if (map.put(m.getName(), m) != null)
				throw new IllegalStateException("Duplicate method " + m.getName() + " (this one declared in " + m.getDeclaringClass().getCanonicalName() + ")");
			m.setAccessible(true);
		}
		return ImmutableMap.copyOf(map);
	}
	
	public static String invocationToString(Method method, Object[] args) {
		StringBuilder callStr = new StringBuilder(method.getName());
		callStr.append('(');
		if (args != null) try {
			callStr.append(Stringsjoin(",", args));
		} catch (Throwable e) {
			callStr.append("argsToStringError: " + e.getMessage());
		}
		callStr.append(')');
		return callStr.toString();
	}
	
	/**
	 * Invoke the named method on the given object with the given args. Only works for instance methods that are not overloaded.
	 * @return The result of the invocation.
	 */
	public static Object invoke(Object o, String method, List<?> args) {
		
		Method m = mapPublicMethodsByName(o.getClass()).get(method);
		if (m == null) throw new IllegalArgumentException("No such method " + o.getClass().getCanonicalName() + "::" + method);
		
		try {
			return m.invoke(o, args.toArray());
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
		
	}

	// TODO move into Strings
	private static String Stringsjoin(String separator, Object parts) {
		if (!parts.getClass().isArray()) {
			throw new IllegalArgumentException("parts is not an array");
		}
		
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (int p = 0; p < Array.getLength(parts); p++) {
			if (first) first = false;
			else b.append(separator);
			b.append(String.valueOf(Array.get(parts, p)));
		}
		
		return b.toString();
		
	}

	/**
	 * @throws IllegalArgumentException if arg is not a valid argument for a parameter of type paramType
	 */
	public static void checkInvokeArg(Class<?> paramType, Object arg) {
		Class<?> argType = arg == null ? null : arg.getClass();

		if (paramType.isPrimitive()) {
			if (argType == null) throw new IllegalArgumentException("arg null is not assignable to primitive " + paramType);

			Class<?> wrapper = Primitives.wrap(paramType);
			if (argType != wrapper) throw new IllegalArgumentException("arg " + argType + " is not unboxable to " + paramType);

		} else if (argType != null && !paramType.isAssignableFrom(argType)) {
			throw new IllegalArgumentException("arg " + argType + " is not assignable to " + paramType);
		}
	}
	
	
}
