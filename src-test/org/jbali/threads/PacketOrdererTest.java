package org.jbali.threads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.junit.Test;

public class PacketOrdererTest {
	
	@Test
	public void testSimple() {
		
		StringBuilder b = new StringBuilder();
		
		PacketOrderer<Long> o = new PacketOrderer<Long>(Function.identity(), p -> {
			b.append(p);
			System.out.println(b.toString());
			if (p == 8) throw new RuntimeException("8-[");
		});
		
		o.inPacket(1L);
		o.inPacket(2L);
		o.inPacket(4L);
		o.inPacket(3L);
		o.inPacket(1L);
		o.inPacket(8L);
		o.inPacket(7L);
		o.inPacket(5L);
		o.inPacket(6L);
		o.inPacket(9L);
		
		assertEquals("123456789", b.toString());
		
	}
	
	@Test
	public void testMultiThread() {
		
		
		AtomicLong tracker = new AtomicLong(0);
		
		PacketOrderer<Long> o = new PacketOrderer<Long>(Function.identity(), p -> {
			if (!tracker.compareAndSet(p-1, p)) {
				fail("Got p but tracker has " + tracker.get());
			}
		});
		
		for (int s = 0; s < 10; s++) {
			final int start = s;
			ThreadPool.execute(() -> {
				for (int n = start; n <= 1000; n+=10) {
					o.inPacket(Long.valueOf(n));
					try {
						Thread.sleep((long) (Math.random() * 5));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});	
		}
		
		while (tracker.get() != 1000) {
			Thread.yield();
		}
		
	}
	
}
