package org.jbali.threads;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jbali.collect.Maps;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class PacketOrderer<P> {
	
	private static final Logger log = LoggerFactory.getLogger(PacketOrderer.class);

	private final Object lock = new Object();

	// state
	private long lastSeq = 0;
	private final Map<Long, P> waiting = Maps.createHash();
	private DateTime waitingSince = null;
	
	// callbacks
	private final Function<P, Long> seqGetter;
	private final Consumer<P> orderedConsumer;

	public PacketOrderer(Function<P, Long> seqGetter, Consumer<P> orderedConsumer) {
		this.seqGetter = seqGetter;
		this.orderedConsumer = orderedConsumer;
	}
	
	public class Status {
		public final int waitingCount;
		public final DateTime waitingSince;
		public final long lastSeq;
		public Status(int waitingCount, DateTime waitingSince, long lastSeq) {
			this.waitingCount = waitingCount;
			this.waitingSince = waitingSince;
			this.lastSeq = lastSeq;
		}
	}

	public Status getStatus() {
		synchronized (lock) {
			return new Status(waiting.size(), waitingSince, lastSeq);
		}
	}
	

	public void setLastSeq(long ls) {
		synchronized (lock) {
			Preconditions.checkState(lastSeq == 0, "lastSeq should only be set once, before receiving the first packet");
			lastSeq = ls;
		}
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
				if (waiting.isEmpty()) waitingSince = null;
			} else if (pSeq <= lastSeq) {
				log.info("inPacket #" + pSeq + " already handled in the past: " + p);
			} else {
				// this packet comes after the expected one, store it for later
//				log.debug("Store");
				if (waiting.isEmpty()) waitingSince = DateTime.now();
				waiting.put(pSeq, p);
			}
		}
	}

	public void latePackets(List<P> events) {
		synchronized (lock) {
			// TODO optimize:
			// - cut off list-head until lastSeq
			// - clear waiting map-head until last of list
			events.forEach(this::inPacket);
		}
	}
	
}
