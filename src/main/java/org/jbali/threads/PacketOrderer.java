package org.jbali.threads;

import com.google.common.base.Preconditions;
import org.jbali.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Thread-safe utility class that takes incoming packets in any order, then queues and orders
 * them based on their sequence number, and passes them on to a consumer when it can do so
 * without creating a gap in the sequence. In this it functions like TCP.
 */
public class PacketOrderer<P> {
	
	private static final Logger log = LoggerFactory.getLogger(PacketOrderer.class);

	private final Object lock = new Object();

	// state
	private long lastSeq = 0;
	private final Map<Long, P> waiting = Maps.createHash();
	private Instant waitingSince = null;
	
	// callbacks
	private final Function<P, Long> seqGetter;
	private final Consumer<P> orderedConsumer;

	public PacketOrderer(Function<P, Long> seqGetter, Consumer<P> orderedConsumer) {
		this.seqGetter = seqGetter;
		this.orderedConsumer = orderedConsumer;
	}
	
	public static class Status {
		public final int waitingCount;
		public final Instant waitingSince;
		public final long lastSeq;
		public Status(int waitingCount, Instant waitingSince, long lastSeq) {
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

	public PacketOrdererState<P> saveState() {
		synchronized (lock) {
			return new PacketOrdererState<>(
					lastSeq,
					waiting.values().stream().collect(Collectors.toList()),
					waitingSince
			);
		}
	}

	public void setLastSeq(long ls) {
		synchronized (lock) {
			Preconditions.checkState(lastSeq == 0, "lastSeq should only be set once, before receiving the first packet");
			lastSeq = ls;
		}
	}

	/**
	 * Takes an incoming packet.
	 */
	public void inPacket(P packet) {
		synchronized (lock) {
			long inSeq = seqGetter.apply(packet);
			if (inSeq == lastSeq+1) {
				// this is the next expected packet, consume it
				while (true) {
					
					try {
						orderedConsumer.accept(packet);
					} catch (Throwable e) {
						log.warn("Error consuming packet #" + inSeq, e);
					}
					// advance the consumed pointer
					lastSeq = inSeq;
					
					// now, check if we also already have the next packet in storage, and handle it if so
					if (waiting.isEmpty()) break;
					P waitingNext = waiting.remove(lastSeq+1);
					if (waitingNext == null) break;
					packet = waitingNext;
					inSeq = lastSeq+1;
				}
				if (waiting.isEmpty()) waitingSince = null;
			} else if (inSeq <= lastSeq) {
				log.info("inPacket #" + inSeq + " already handled in the past: " + packet);
			} else {
				// this packet comes after the expected one, store it for later
				if (waiting.isEmpty()) waitingSince = Instant.now();
				waiting.put(inSeq, packet);
			}
		}
	}

	/**
	 * Takes a batch of incoming packets. Is functionally equivalent to calling {@link #inPacket(Object)} for each
	 * packet, but may perform faster.
	 */
	public void latePackets(List<P> batch) {
		synchronized (lock) {
			// TODO optimize:
			// - cut off list-head until lastSeq
			// - clear waiting map-head until last of list
			batch.forEach(this::inPacket);
		}
	}
	
}
