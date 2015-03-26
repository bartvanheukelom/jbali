package org.jbali.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

public class MapTransform {
	
	// --- full map (one transformer) ---
	
	public static <OK,OV,NV,NK> ImmutableMap<NK, NV> transformMap(Collection<Entry<OK, OV>> entries, Function<Entry<OK, OV>, Entry<NK, NV>> transform) {
		final ImmutableMap.Builder<NK, NV> newMap = ImmutableMap.builder();
		entries.stream()
			.map(transform)
			.forEach(newMap::put);
		return newMap.build();
	}
	
	public static <OK,OV,NK,NV> ImmutableMap<NK,NV> transformMap(Map<OK,OV> in,	Function<Entry<OK,OV>, Entry<NK,NV>> transform) {
		return transformMap(in.entrySet(), transform);
	}
	
	// --- full map (2 transformers) ---
	
	public static <OK,OV,NK,NV> ImmutableMap<NK,NV> transformMap(Collection<Entry<OK, OV>> entries,	Function<OK, NK> transformKey, Function<OV, NV> transformValue) {
		return transformMap(entries, e -> SimpleMapEntry.of(transformKey.apply(e.getKey()), transformValue.apply(e.getValue())));
	}
	
	public static <OK,OV,NK,NV> ImmutableMap<NK,NV> transformMap(Map<OK,OV> in,	Function<OK, NK> transformKey, Function<OV, NV> transformValue) {
		return transformMap(in.entrySet(), transformKey, transformValue);
	}
	
	// --- only keys ---
	
	public static <OK,OV,NK> ImmutableMap<NK,OV> transformKeys(Collection<Entry<OK, OV>> entries, Function<OK, NK> transformKey) {
		return transformMap(entries, e -> SimpleMapEntry.of(transformKey.apply(e.getKey()), e.getValue()));
	}
	
	public static <OK,OV,NK> ImmutableMap<NK,OV> transformKeys(Map<OK,OV> in, Function<OK, NK> transformKey) {
		return transformKeys(in.entrySet(), transformKey);
	}
	
	// --- only values ---
	
	public static <OK,OV,NV> ImmutableMap<OK,NV> transformValues(Collection<Entry<OK, OV>> entries, Function<OV, NV> transformValue) {
		return transformMap(entries, e -> SimpleMapEntry.of(e.getKey(), transformValue.apply(e.getValue())));
	}
	
	public static <OK,OV,NV> ImmutableMap<OK,NV> transformValues(Map<OK,OV> in, Function<OV, NV> transformValue) {
		return transformValues(in.entrySet(), transformValue);
	}
}
