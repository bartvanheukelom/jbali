package org.jbali.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class Methods {
	
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
				throw new IllegalStateException("Duplicate method " + m.getName());
			m.setAccessible(true);
		}
		return ImmutableMap.copyOf(map);
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

	
}
