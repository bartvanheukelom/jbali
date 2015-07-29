package org.jbali.serialize;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;

import org.jbali.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class JavaJsonSerializerTest {

	@Test
	public void testTwice() throws Exception {
		
		Arrays.asList(
				12, 12L, (short) 12, (byte) 12, 12d, 12f, '1',
				8239492384723894L, 8912389912389823.32482934,
				true, false,
				null,
				"Hi",
				"This is a longer text",
				"Some bytes  123891238192389".getBytes(),
				new LocalDate(),
				new Date(),
				new DateTime(),
				ImmutableList.of("12", new LocalDate(), true, 12)
//				Maybe.definitely(12), Maybe.unknown()
		).forEach(v -> {
			Object s = JavaJsonSerializer.serialize(v);
			System.out.println(vClass(v) + " " + v + " => " + JSONObject.valueToString(s));
			Object us = JavaJsonSerializer.unserialize(s);
			System.out.println("<= " + vClass(us) + " " + us);
			assertEquals(vClass(v), vClass(us));
			if (v instanceof byte[])
				assertArrayEquals((byte[]) v, (byte[]) us);
			else
				assertEquals(v, us);
		});
		
	}
	
	private static String vClass(Object v) {
		return v == null ? "null" : v.getClass().getSimpleName();
	}
	
}
