package org.jbali.jmsrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.jbali.json.JSONArray;
import org.jbali.threads.ThreadPool;
import org.joda.time.LocalDate;
import org.junit.Test;

public class TextMessageServiceTest {
	
	public interface EP {
		void setVal(String v);
		void eur();
		void tooMany(int a);
		void tooFew(int a, Object b);
		Object echo(Object a);
		void nestedLocalFail();
	}
	
	public interface LocalEP {
		void setVal(int v);
		void tooMany(int a, int b);
		void tooFew(int a);
		void eur();
		void nope();
		Object echo(Object a);
		@Override
		boolean equals(Object obj);
		void localFail();
		void nestedLocalFail();
	}

	static class Endpoint implements EP {
		
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
				step1();
			} catch (Exception e) {
				throw new RuntimeException("LOLOL", e);
			}
			
		}
		
		private static void step1() {
			step2();
		}
		private static void step2() {
			throw new IllegalArgumentException();
		}
		
		@Override public Object echo(Object a) {
			return a;
		}
		
		@Override public void tooFew(int a, Object b) {}
		@Override public void tooMany(int a) {}
		
		@Override
		public void nestedLocalFail() {
			throw new TextMessageServiceClientException("MUHAHAHA");
		}
		
	}
	
	@Test
	public void testRaw() throws Exception {
		
		System.out.println("==================== testRaw =====================");
		
		Endpoint ep = new Endpoint();
		
		TextMessageService svc = new TextMessageService(ep);
		
		String resp;
		
		// errors
		
		resp = svc.handleRequest("THISISNOTJSON");
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);
		
		resp = svc.handleRequest(JSONArray.create("", "").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);
		
		resp = svc.handleRequest(JSONArray.create("asdasd", "").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);
		
		resp = svc.handleRequest(JSONArray.create("eur").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 0);
		
		// success
		
		resp = svc.handleRequest(JSONArray.create("setVal", "HI").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 1);
		assertEquals("HI", ep.val);
		
		// too few
		
		resp = svc.handleRequest(JSONArray.create("setVal").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 1);
		assertEquals(null, ep.val);
		
		// too many
		
		resp = svc.handleRequest(JSONArray.create("setVal", "O", "HI").toString());
		assertTrue(resp, new JSONArray(resp).getInt(0) == 1);
		assertEquals("O", ep.val);
		
		
	}
	
	@Test
	public void testClient() throws Exception {
		
		System.out.println("==================== testClient =====================");
		
		Endpoint ep = new Endpoint();
		
		TextMessageService svc = new TextMessageService(ep);
		LocalEP client = TextMessageServiceClient.create(LocalEP.class, r -> {
			System.out.println("submit " + r);
			if (r.startsWith("[\"localFail")) {
				throw new RuntimeException("Internet fail! Yeah, internet...");
			}
			
			// simulate remote request
			try {
				return ThreadPool.submit(() -> {
					Thread.sleep(300);
					return svc.handleRequest(r);
				}).get();
			} catch (Exception e) {
				// handleRequest should not throw
				System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEK!!!");
				throw new RuntimeException(e);
			}
			
		});
		
		System.out.println("----- toString, hashCode, equals -----");
		assertEquals("TextMessageServiceClient[LocalEP]", client.toString());
		assertEquals(System.identityHashCode(client), client.hashCode());
		assertEquals(client, client);
		assertFalse(client.equals(TextMessageServiceClient.create(LocalEP.class, r -> null)));
		
		// errors

		try {
			client.localFail();
			fail();
		} catch (TextMessageServiceClientException e) {
			assertTrue(e.getCause().getMessage().startsWith("Internet fail"));
		}
		
		try {
			client.nestedLocalFail();
			fail();
		} catch (TextMessageServiceClientException e) {
			assertEquals("MUHAHAHA", e.getMessage());
		}
		
		System.out.println("----- nope -----");
		try {
			client.nope();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NoSuchElementException);
		}
		
		System.out.println("----- eur -----");
		try {
			client.eur();
			fail();
		} catch (RuntimeException e) {
			e.printStackTrace();
			try {
				assertEquals("LOLOL", e.getMessage());
				assertTrue(e.getCause() instanceof IllegalArgumentException);
			} catch (AssertionError ae) {
				throw ae;
			}
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
		LocalDate d = new LocalDate();
		assertEquals(d, client.echo(d));
		
		// too few/many		
		client.tooFew(12);
		client.tooMany(12, 13);
		
		// return
		
		
		
	}
	
}
