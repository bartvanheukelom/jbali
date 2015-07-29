package org.jbali.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jbali.jmsrpc.TextMessageService;
import org.jbali.json.JSONArray;
import org.junit.Test;

public class TextMessageServiceTest {
	
	public interface EP {
		void setVal(Object v);
		void eur();
	}

	static class Endpoint implements EP {
		
		public Object val = null;
		
		@Override
		public void setVal(Object v) {
			val = v;
		}
		
		@Override
		public void eur() {
			throw new RuntimeException("LOLOL");
		}
		
		@Override
		public String toString() {
			return "HI";
		}
		
	}
	
	@Test
	public void testStuff() throws Exception {
		
		Endpoint ep = new Endpoint();
		
		TextMessageService svc = new TextMessageService(ep);
		
		String resp;
		
		// errors
		
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
	
}
