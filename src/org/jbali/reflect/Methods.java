package org.jbali.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class Methods {

	public static boolean isPublic(Method m) {
		return Modifier.isPublic(m.getModifiers());
	}
	public static boolean isStatic(Method m) {
		return Modifier.isStatic(m.getModifiers());
	}
	
	public static ImmutableMap<String, Method> mapPublicMethodsByName(Class<?> c) {
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

	
}
