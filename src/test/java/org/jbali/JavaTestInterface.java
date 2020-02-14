package org.jbali;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public interface JavaTestInterface {

	/**
	 * KType.isMarkedNullable should return false for these.
	 */
	void notNull(
			int a,
			Integer b,

			// primitive cannot be nullable
			@SuppressWarnings("NullableProblems")
			@Nullable int c,

			// annotations erased at runtime
			@org.jetbrains.annotations.NotNull Integer d,
			@org.jetbrains.annotations.Nullable Integer e
	);

	/**
	 * KType.isMarkedNullable should return true for these.
	 */
	void nullable(
			@javax.annotation.Nullable Integer x,

			// these contradict, but
			// jetbrains annotation has no effect at runtime
			@SuppressWarnings("NullableProblems")
			@javax.annotation.Nullable
			@org.jetbrains.annotations.NotNull Integer y
	);

}
