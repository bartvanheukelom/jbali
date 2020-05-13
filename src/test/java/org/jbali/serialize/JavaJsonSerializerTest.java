package org.jbali.serialize;

import com.google.common.collect.ImmutableList;
import kotlin.collections.MapsKt;
import kotlinx.serialization.json.*;
import org.jbali.collect.Maps;
import org.jbali.json.JSONObject;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
				LocalDate.now(),
				new Date(),
				Instant.now(),
				ImmutableList.of("12", LocalDate.now(), true, 12),
				JsonNull.INSTANCE,
				new JsonObject(Maps.create(
						// to assert equal, this order must be preserved and the number must be an int
						"missingno", JsonNull.INSTANCE,
						"obj", new JsonObject(MapsKt.emptyMap()),
						"foo", new JsonLiteral("bar"),
						"blub", new JsonLiteral(12),
						"arrrrrrr", new JsonArray(Arrays.asList(
								new JsonLiteral("you are"),
								new JsonLiteral("a pirate")
						))
				))
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
