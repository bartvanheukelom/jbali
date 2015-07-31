package org.jbali.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.jbali.jmsrpc.TextMessageService;
import org.jbali.jmsrpc.TextMessageServiceClient;
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
	}
	
	public interface LocalEP {
		void setVal(int v);
		void tooMany(int a, int b);
		void tooFew(int a);
		void eur();
		void nope();
		Object echo(Object a);
	}

	static class Endpoint implements EP {
		
		public String val = null;
		
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
		
		@Override
		public String toString() {
			return "HI";
		}
		
		@Override public Object echo(Object a) {
			return a;
		}
		
		@Override public void tooFew(int a, Object b) {}
		@Override public void tooMany(int a) {}
		
	}
	
	@Test
	public void testRaw() throws Exception {
		
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
		
		Endpoint ep = new Endpoint();
		
		TextMessageService svc = new TextMessageService(ep);
		LocalEP client = TextMessageServiceClient.create(LocalEP.class, r -> {
			try {
				// simulate remote request
				return ThreadPool.submit(() -> {
					Thread.sleep(300);
					return svc.handleRequest(r);
				}).get();
			} catch (Exception e) {
				// handleRequest should not throw
				throw new RuntimeException(e.getCause());
			}
		});
		
		// errors
		
		try {
			client.nope();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NoSuchElementException);
		}
		
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
