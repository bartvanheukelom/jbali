package org.jbali.util;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

public class EnumToolsTest {

	enum Mune {
		FOO, BAR, MOOMIN
	}

	@Test
	public void parseEnumSet() {
		assertEquals(EnumSet.noneOf(Mune.class), EnumTools.parseEnumSet(Mune.class, ""));
		assertEquals(EnumSet.noneOf(Mune.class), EnumTools.parseEnumSet(Mune.class, ","));
		assertEquals(EnumSet.noneOf(Mune.class), EnumTools.parseEnumSet(Mune.class, " "));

		assertEquals(EnumSet.allOf(Mune.class), EnumTools.parseEnumSet(Mune.class, "*"));
		assertEquals(EnumSet.allOf(Mune.class), EnumTools.parseEnumSet(Mune.class, ", *"));

		assertEquals(EnumSet.of(Mune.FOO), EnumTools.parseEnumSet(Mune.class, "FOO"));
		assertEquals(EnumSet.of(Mune.FOO), EnumTools.parseEnumSet(Mune.class, "Foo"));
		assertEquals(EnumSet.of(Mune.FOO, Mune.BAR), EnumTools.parseEnumSet(Mune.class, "Foo, BAR"));
		assertEquals(EnumSet.of(Mune.FOO, Mune.BAR), EnumTools.parseEnumSet(Mune.class, "BAR,,,foo"));
		assertEquals(EnumSet.of(Mune.FOO, Mune.BAR), EnumTools.parseEnumSet(Mune.class, "BAR,,,foo,CRAP"));

		assertEquals(EnumSet.of(Mune.FOO, Mune.BAR), EnumTools.parseEnumSet(Mune.class, "*, -MOOMIN"));
		assertEquals(EnumSet.of(Mune.FOO, Mune.BAR), EnumTools.parseEnumSet(Mune.class, "*, -BAR, *, -MOOMIN, ,MOOMIN, -m o o  MIN"));
	}

}