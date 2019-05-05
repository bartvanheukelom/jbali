package org.jbali.websocket;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbali.threads.ThreadPool;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadWrappedWebSocket {
	
	public static final List<ThreadWrappedWebSocket> instances = Collections.synchronizedList(Lists.newArrayList());

	private static final Log log = LogFactory.getLog(ThreadWrappedWebSocket.class);
	
	private final WebSocket s;
	private final Lock writeLock = new ReentrantLock(true);
	private volatile Listener listener;
	
	private final Lock readingThreadLock = new ReentrantLock(true);
	private Future<?> readingThread = null;
	
	private final boolean forkReceived;

	public interface Listener {
		/**
		 * A packet was received.
		 * @param packet The packet.
		 */
		void received(WebSockets.Message msg);
		/**
		 * The socket was closed.
		 * @param data Information about the closure.
		 */
		void closed(WebSocket.CloseData data);
	}
	
	public ThreadWrappedWebSocket(WebSocket s, boolean forkReceived, Listener l) {
		this(s, forkReceived);
		setListener(l);
	}
	
	public ThreadWrappedWebSocket(WebSocket s, boolean forkReceived) {
		this.s = s;
		this.forkReceived = forkReceived;
		instances.add(this);
	}
	
	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public String toString() {
		return "[ThreadWrapped(" + s + ")]";
	}
	
	public void readyForPackets() {
		readingThreadLock.lock();
		try {
			Preconditions.checkState(readingThread == null);
			readingThread = ThreadPool.submit(() -> readInThisThread(), "Reading " + this);
		} finally {
			readingThreadLock.unlock();
		}
	}
	
	private void readInThisThread() {
		while (!Thread.interrupted()) {

			// read a packet
			final WebSockets.Message packet;
			try {
				packet = s.read();
			} catch (WebSocket.ClosedException e) {
				listenerClosed(e.getCd());
				break;
			}
			
			// pass it up
			if (forkReceived) {
				ThreadPool.execute(new Runnable() { @Override public void run() {
					listenerReceived(packet);	
				}});
			} else {
				listenerReceived(packet);
			}

		}
	}

	private void listenerReceived(WebSockets.Message m) {
		try {
			final Listener l = listener;
			if (l != null) l.received(m);
		} catch (Throwable e) {
			log.warn("Error while calling received() on AsyncPacketSocket listener", e);
		}
	}

	private void listenerClosed(final WebSocket.CloseData packetSocketCloseData) {
		ThreadPool.execute(new Runnable() { @Override public void run() {
			try {
				log.info("Connection closed (" + ThreadWrappedWebSocket.this + "): " + packetSocketCloseData);
				final Listener l = listener;
				if (l != null) l.closed(packetSocketCloseData);
			} catch (Throwable e) {
				log.warn("Error while calling closed() on AsyncPacketSocket listener", e);
			}
		}});
	}

	public void send(WebSockets.Message packet) {
//		try {
//			ThreadTimeout.lock(writeLock); TODO
//		} catch (InterruptedException | TimeoutException e) {
//			throw Exceptions.wrap(e);
//		}
		writeLock.lock();
		try {
			s.write(packet);
		} finally {
			writeLock.unlock();
		}
	}

	public void close() {
		close(null);
	}
	
	public void close(Object extra) {
		if (s.isOpen()) listenerClosed(s.close(extra));
	}

	public boolean isOpen() {
		return s.isOpen();
	}
	
	
}
