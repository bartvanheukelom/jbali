package org.jbali.collect;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple map that stores data as a list of entries. Random access is slow, O(n), but inserting and iterating is fast.
 * This map implementation is intended for maps that are only written to (often just once) and then
 * iterated over, for example when preparing a message to be sent over a network connection.
 * The iteration order equals the insertion order. Its behaviour when duplicate keys is
 * undefined (it can be defined, but ListMap simply should not be used that way).
 * @author Bart van Heukelom
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 */
public class ListMap<K,V> extends AbstractMap<K, V> {

	private class Item implements Entry<K, V> {

		private K key;
		private V val;

		public Item(K key, V val) {
			this.key = key;
			this.val = val;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return val;
		}

		@Override
		public V setValue(V value) {
			return val = value;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ListMap.Item) {
				return key.equals(((ListMap<K,V>.Item)obj).key);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}
		
	}
	
	private final List<Entry<K, V>> data;
	private final AbstractSet<Entry<K, V>> eset = new AbstractSet<Entry<K, V>>() {
		@Override public Iterator<Entry<K, V>> iterator() {
			return data.iterator();
		}
		@Override public int size() {
			return data.size();
		}
	};
	
	public ListMap() {
		data = new ArrayList<Entry<K, V>>();
	}
	
	public ListMap(Map<K, V> source) {
		data = new ArrayList<Entry<K, V>>(source.entrySet());
	}

	public ListMap(int length) {
		data = new ArrayList<Entry<K, V>>(length);
	}

	@Override public Set<Entry<K, V>> entrySet() {
		return eset;
	}
	
	@Override public int size() {
		return data.size();
	}
	
	@Override public void clear() {
		data.clear();
	}
	
	@Override public boolean isEmpty() {
		return data.isEmpty();
	}
	
	@Override
	public V put(K key, V value) {
		data.add(new Item(key, value));
		return value;
	}
	
	

}
