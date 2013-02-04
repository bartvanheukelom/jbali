package org.jbali.collect;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Maps {

	/**
	 * Use this method to create and fill a map in one statement. Example:
	 * <pre>Maps.create(
	 *	"foo", "bar",
	 *	"this", "that",
	 *	"value", "worthless",
	 *);</pre>
	 *Or when mixing types:<br/>
	 *<pre>Maps.&lt;String,Object&gt;create(
	 *	"foo", "bar",
	 *	"value", 13,
	 *);</pre>
	 *<p>The created map is an instance of {@link ListMap}. Use
	 *{@link Maps#createHash(Object, Object, Object...)}
	 *or {@link #create(Class, Object, Object, Object...)}
	 *to create a {@link HashMap} or a different map class, respectively.</p>
	 * @param <K> The type of the keys
	 * @param <V> The type of the values
	 * @param key1 The first key (will be used for type inference)
	 * @param val1 The first value (likewise)
	 * @param rest The other keys and values, as in the example
	 * @return The created and filled map
	 */
	public static <K,V> ListMap<K,V> create(K key1, V val1, Object...rest) {
		ListMap<K, V> map = new ListMap<K, V>();
		fillMap(key1, val1, rest, map);
		return map;
	}
	
	public static <K,V> Map<K, V> merge(Map<? extends K, ? extends V> ... sources) {
		HashMap<K, V> nw = new HashMap<K, V>();
		for (Map<? extends K, ? extends V> map : sources) {
			nw.putAll(map);
		}
		return nw;
	}
		
	/**
	 * Merge two maps into one {@link HashMap}. In case of key overlap, <code>b</code>'s
	 * values survive.
	 * @param <K>
	 * @param <V>
	 * @param a
	 * @param b
	 * @return
	 */
	public static <K,V> Map<K, V> merge(Map<? extends K, ? extends V> a, Map<? extends K, ? extends V> b) {
		HashMap<K, V> nw = new HashMap<K, V>(a);
		nw.putAll(b);
		return nw;
	}
	
	
	/**
	 * Like {@link #create(Object, Object, Object...)}, but returns a
	 * {@link HashMap} instead of a {@link ListMap}.
	 * @see Maps#create(Object, Object, Object...)
	 */
	public static <K,V> HashMap<K,V> createHash(K key1, V val1, Object...rest) {
		HashMap<K, V> map = new HashMap<K, V>();
		fillMap(key1, val1, rest, map);
		return map;
	}
	
	public static <K,V> HashMap<K,V> createHash() {
		return new HashMap<K, V>();
	}
	
	/**
	 * Like {@link #create(Object, Object, Object...)}, but returns a
	 * map of the given type instead of a {@link ListMap}.
	 * @see #create(Object, Object, Object...)
	 */
	public static <K,V> Map<K,V> create(Class<? extends Map<K, V>> type, K key1, V val1, Object...rest) {
		try {
			Map<K, V> map = type.newInstance();
			fillMap(key1, val1, rest, map);
			return map;
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void fillMap(Object key1, Object val1, Object[] rest, Map map) {
		
		if (rest.length % 2 != 0) throw new IllegalArgumentException("Number of parameters must be even");
		
		map.put(key1, val1);
		for (int i = 0; i < rest.length; i+=2) {
			if (rest[i] != null)
				map.put(rest[i], rest[i+1]);
		}
		
	}
	
	/**
	 * Create a {@link HashMap} of an enum, mapping the values of a field in the enum to the enum values.
	 * @param <E>
	 * @param enumType The enum class
	 * @param field The name of the field to use as key
	 * @return
	 */
	public static <E extends Enum<?>> Map<?, E> enumMap(Class<E> enumType, String field) {
		try {
			Field f = enumType.getDeclaredField(field);
			Object[] values = (Object[]) enumType.getMethod("values").invoke(null);
			HashMap<Object, E> m = new HashMap<Object, E>(values.length);
			for (Object o : values) {
				m.put(f.get(o), enumType.cast(o));
			}
			return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
