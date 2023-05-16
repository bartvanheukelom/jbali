package org.jbali.jmsrpc;

import kotlin.Unit;
import org.jbali.json.JSONArray;
import org.jbali.json.JSONObject;
import org.jbali.threads.ThreadPool;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.Assert.*;

public class TextMessageServiceTest {

	// these interfaces are "the same" but different versions,
	// to simulate backwards-compatibility testing.
	// methods with differences are listed last.
	public interface ServerEP {
		
		// SAME //
		void eur();
		Object echo(Object a);
		void nestedLocalFail();
		@KoSe
		TestKoseData kose(@Nullable TestKoseData data, @JJS TestJavaSerData mixer);
		@KoSeReturn void koseVoid();
		@KoSe @Nonnull UUID javaUuidType(); // was UUID! i.e. no nullability info, but made that an error
		
		// DIFFERENT //
		void setVal(String v);
		void foo(int a);
		void bar(int a, Object b);
		
	}
	public interface LocalEP {
		
		// SAME //
		void eur();
		Object echo(Object a);
		void nestedLocalFail();
		@KoSe
		TestKoseData kose(@Nullable TestKoseData data, @JJS TestJavaSerData mixer);
		@KoSeReturn void koseVoid();
		@KoSe @Nonnull UUID javaUuidType(); // UUID! i.e. no nullability info
		
		// DIFFERENT //
		void setVal(int v); // different param type
		void foo(int a, int b); // has 1 more parameter
		void bar(int a); // has 1 less parameter
		void nope(); // missing in server version
		
		@Override
		boolean equals(Object obj); // ?
		
		void localFail(); // hackyhack, will never be submitted
		
	}

	static class Endpoint implements ServerEP {
		
		public String val = null;
		
		@Override
		public String toString() {
			throw new AssertionError();
		}
		@Override
		public boolean equals(Object obj) {
			throw new AssertionError(); 
		}
		@Override
		public int hashCode() {
			throw new AssertionError(); 
		}
		
		@Override
		public void setVal(String v) {
			val = v;
		}
		
		@Override
		public void eur() {
			try {
				callsThrowing();
			} catch (Exception e) {
				throw new RuntimeException("Wrapped e", e);
			}
			
		}
		
		private static void callsThrowing() {
			throwsIAE();
		}
		private static void throwsIAE() {
			throw new IllegalArgumentException();
		}
		
		@Override public Object echo(Object a) {
			return a;
		}
		
		@Override public void bar(int a, Object b) {}
		@Override public void foo(int a) {}
		
		@Override
		public void nestedLocalFail() {
			throw new TextMessageServiceClientException("MUHAHAHA");
		}

		public void notInInterface() {}
		
		@Override
		public TestKoseData kose(@Nullable TestKoseData data, TestJavaSerData mixer) {
			if (data != null && data.getFibonacciMaybe().isEmpty()) throw new IllegalArgumentException("no fib?");
			return data != null ? data : new TestKoseData(Arrays.asList(1, 2, 3));
		}
		
		@Override
		public void koseVoid() {}
		
		@Override
		public @Nonnull UUID javaUuidType() {
			return new UUID(77777, 888888);
		}
	}
	
	@Test
	public void testRaw() throws Exception {
		
		System.out.println("==================== testRaw =====================");
		
		Endpoint ep = new Endpoint();
		TextMessageService<?> svc = new TextMessageService<>(ServerEP.class, "TestSVCRaw", ep);
		
		String resp;



		// ======= errors ========= //

		// invalid JSON
		resp = svc.handleRequest("THISISNOTJSON");
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);

		// no method name
		resp = svc.handleRequest(JSONArray.create("").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);

		// unexisting method
		resp = svc.handleRequest(JSONArray.create("asdasd").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);

		// throws error
		resp = svc.handleRequest(JSONArray.create("eur").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);

		// calling a method that the endpoint has but the interface has not
		resp = svc.handleRequest(JSONArray.create("notInInterface").toString());
		assertEquals(0, new JSONArray(resp).getInt(0));



		// ********* success ********* //
		
		resp = svc.handleRequest(JSONArray.create("setVal", JSONObject.create("v", "HI")).toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 1);
		assertEquals("HI", ep.val);

		// even case insensitive
		resp = svc.handleRequest(JSONArray.create("SETval", JSONObject.create("v", "HI2")).toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 1);
		assertEquals("HI2", ep.val);
		
		// too few
//		resp = svc.handleRequest(JSONArray.create("setVal").toString());
//		assertTrue(resp, new JSONArray(resp).getInt(0) == 1);
//		assertEquals(null, ep.val);
		
		// too many
		resp = svc.handleRequest(JSONArray.create("setVal", JSONObject.create(
				"v", "O",
				"extra", "HI"
		)).toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 1);
		assertEquals("O", ep.val);
		
		
	}
	
	@Test
	public void testClient() throws Exception {
		
		System.out.println("==================== testClient =====================");
		
		Endpoint ep = new Endpoint();
		
		TextMessageService<?> svc  = new TextMessageService<>(ServerEP.class, "TestSVC", ep);
		TextMessageService<?> svcK = new TextMessageService<>(TMSKotlinIface.class, "TestSVCK", TMSKotlinEndpoint.INSTANCE);
		
		LocalEP client = TextMessageServiceClient.create(LocalEP.class, r -> {
			System.out.println("submit " + r);
			if (r.startsWith("[\"localFail")) {
				throw new RuntimeException("Internet fail! Yeah, internet...");
			}
			
			// simulate remote request
			try {
				return ThreadPool.submit(() -> {
//					Thread.sleep(300);
					return svc.handleRequest(r);
				}).get();
			} catch (Exception e) {
				// handleRequest should not throw
				System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEK!!!");
				throw new RuntimeException(e);
			}
			
		});
		TMSKotlinIfaceOlder clientK = TextMessageServiceClient.create(TMSKotlinIfaceOlder.class, svcK::handleRequest);
		
		System.out.println("----- toString, hashCode, equals -----");
		assertEquals("TextMessageServiceClient[LocalEP].blocking", client.toString());
		assertEquals(System.identityHashCode(client), client.hashCode());
		assertEquals(client, client);
		assertFalse(client.equals(TextMessageServiceClient.create(LocalEP.class, r -> null)));
		client.koseVoid();
		assertEquals(new UUID(77777, 888888), client.javaUuidType());
		
		// errors

		try {
			client.localFail();
			fail();
		} catch (TextMessageServiceClientException e) {
			assertTrue(e.getCause().getMessage().startsWith("Internet fail"));
		}
		
		// remote will return a TextMessageServiceClientException, which should be wrapped in RuntimeException locally
		try {
			client.nestedLocalFail();
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getCause() instanceof TextMessageServiceClientException);
			assertEquals("MUHAHAHA", e.getCause().getMessage());
		}
		
		System.out.println("----- nope -----");
		try {
			client.nope();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NoSuchElementException);
			StackTraceElement[] stack = e.getStackTrace();
			findBarrier: {
				e.printStackTrace();
				for (int i = 0; i < stack.length; i++) {
					if (stack[i].getClassName().startsWith("====")) {
						assertEquals(TextMessageServiceTest.class.getName(), stack[i+2].getClassName());
						break findBarrier;
					}
				}
				fail();
			}
		}
		
		System.out.println("----- eur -----");
		try {
			client.eur();
			fail();
		} catch (RuntimeException e) {
			e.printStackTrace();
			assertEquals("Wrapped e", e.getMessage());
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
		
		// wrong type		
		try {
			client.setVal(1337);
			fail();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e instanceof IllegalArgumentException);
		}
		
		// success & return
		LocalDate d = LocalDate.now();
		assertEquals(d, client.echo(d));
		
		// too few/many, should still be compatible
		client.foo(12, 13);
//		client.bar(12); - not since move from args array to args object
		
		// too few but it's nullable
//		assertNull(clientK.withNullable()); - not since move from args array to args object
		// too few but it has a default value
		assertEquals("hello", clientK.withDefault());
		
		// kose
		TestKoseData td = new TestKoseData(
				Arrays.asList(5, 5, 5) // hey, it said maybe
		);
		assertEquals(td, client.kose(td, TestJavaSerData.INSTANCE));
		assertNotNull(client.kose(null, TestJavaSerData.INSTANCE));
		try {
			client.kose(new TestKoseData(Collections.emptyList()), TestJavaSerData.INSTANCE); // should throw for empty list
			fail();
		} catch (RuntimeException e) {
			assertEquals("no fib?", e.getMessage());
		}
		
		assertEquals(78, clientK.jjsEcho(new JavaSerThingy(78)).getX());
		assertEquals(32, clientK.koseEcho(new KoSeThingy(32)).getX());
		assertEquals(44, clientK.openJjsEcho(new JavaSerThingy(44)).getX());
		// TODO move the rest above into this:
		TMSKotlinIfaceKt.runTmsKotlinIfaceTest(clientK);
		
		clientK.returningUnit(); // simply should not throw
		assertEquals(Unit.INSTANCE, clientK.returningUnitOrNull(true));
		assertNull(clientK.returningUnitOrNull(false));
		
		// returns Int, but we expected Unit. Int should be succesfully ignored for backwards-compatiblity.
		clientK.returnChangedToSomething();
		// changed Long -> Int, should be able to read as Long
		assertEquals(55L, clientK.returnNumberNarrowed());
		// changed String? -> String, should be able to read
		assertEquals("definitely a piece of text", clientK.returnTypeNarrowed());

	}
	
}
