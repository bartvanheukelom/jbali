package org.jbali.threads;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jbali.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketOrderer<P> {
	
	private static final Logger log = LoggerFactory.getLogger(PacketOrderer.class);

	private final Object lock = new Object();
	private final Map<Long, P> waiting = Maps.createHash();
	private long lastSeq = 0;
	private final Function<P, Long> seqGetter;
	private final Consumer<P> orderedConsumer;

	public PacketOrderer(Function<P, Long> seqGetter, Consumer<P> orderedConsumer) {
		this.seqGetter = seqGetter;
		this.orderedConsumer = orderedConsumer;
	}

	public void inPacket(P p) {
		synchronized (lock) {
			long pSeq = seqGetter.apply(p);
//			log.debug("inPacket #" + pSeq);
			if (pSeq == lastSeq+1) {
//				log.debug("Expected");
				// this is the next expected packet, consume it
				while (true) {
					
					try {
						orderedConsumer.accept(p);
					} catch (Throwable e) {
						log.warn("Error consuming packet #" + pSeq, e);
					}
					// advance the consumed pointer
					lastSeq = pSeq;
					
					// now, check if we also already have the next packet in storage, and handle it if so
//					log.debug("Waiting: " + waiting.keySet());
					if (waiting.isEmpty()) break;
//					log.debug("Have waiting");
					P waitingNext = waiting.remove(lastSeq+1);
					if (waitingNext == null) break;
					p = waitingNext;
					pSeq = lastSeq+1;
//					log.debug("Waiting is next #" + pSeq);
					
				}
			} else if (pSeq <= lastSeq) {
				log.info("inPacket #" + pSeq + " already handled in the past: " + p);
			} else {
				// this packet comes after the expected one, store it for later
//				log.debug("Store");
				waiting.put(pSeq, p);
			}
		}
	}
	
}
