package org.jbali.collect;

import java.util.Map.Entry;

public class SimpleMapEntry<K,V> implements Entry<K, V> {

	public final K key;
	public final V value;
	
	public SimpleMapEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}
	
	public static <C,F> SimpleMapEntry<C,F> of(C key, F value) {
		return new SimpleMapEntry<>(key, value);
	}
	
	@Override
	public K getKey() {
		return key;
	}
	@Override
	public V getValue() {
		return value;
	}
	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}
	
	
	
}
