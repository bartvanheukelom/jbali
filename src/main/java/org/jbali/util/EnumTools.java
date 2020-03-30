package org.jbali.util;

import com.google.common.base.Enums;
import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Stream;

public class EnumTools {

	@SuppressWarnings("unchecked")
	public static EnumSet<?> setFromNamesUnsafe(Class enumClass, Stream<String> names) {
		EnumSet result = EnumSet.noneOf(enumClass);
		names.forEach(n -> {
			Optional val = Enums.getIfPresent(enumClass, n);
			if (val.isPresent()) result.add(val.get());
			else throw new IllegalArgumentException(enumClass + " has no value " + n);
		});
		return result;
	}

	@SuppressWarnings("unchecked")
	public static EnumSet<?> parseEnumSetUnsafe(Class enumClass, String str) {
		if (!enumClass.isEnum()) throw new IllegalArgumentException(enumClass + " is not an enum class");
		return parseEnumSet(enumClass, str);
	}

	public static <E extends Enum<E>> EnumSet<E> parseEnumSet(Class<E> enumClass, String str) {
		EnumSet<E> rights = EnumSet.noneOf(enumClass);
		// split and clean input string
		Arrays.stream((str != null ? str : "").split(","))
				.map(s -> s.replaceAll("\\s+", ""))
				.filter(s -> !s.isEmpty())
				// TODO support enums that use lowercase
				.map(s -> s.toUpperCase())
				// handle parts
				.forEach(s -> {
					// '*' adds all rights
					if (s.equals("*")) rights.addAll(Arrays.asList(enumClass.getEnumConstants()));
					// '-RIGHT' removes a right
					boolean remove = s.startsWith("-");
					if (remove) s = s.substring(1);
					// parse the enum name
					Optional<E> val = Enums.getIfPresent(enumClass, s);
					if (!val.isPresent()) return;
					// add/remove it
					if (remove) rights.remove(val.get());
					else rights.add(val.get());
				});
		return rights;
	}

}
