package org.jbali.collect;

import java.util.List;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public abstract class IntRange {

	// TODO implement without a list
	public static IntStream create(int start, int end) {
		Preconditions.checkArgument(end >= start);
		List<Integer> l = Lists.newArrayListWithExpectedSize(end-start);
		for (int i = start; i < end; i++) l.add(i);
		return l.stream().mapToInt(Integer::intValue);
	}

}
