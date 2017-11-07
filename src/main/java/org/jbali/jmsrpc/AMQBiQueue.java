package org.jbali.jmsrpc;

import java.util.function.Consumer;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQBiQueue {
	
	private static final Logger log = LoggerFactory.getLogger(AMQBiQueue.class);
	
	private final Session sess;
	
	private final Destination inQueue;
	private final Destination outQueue;
	
	private final MessageProducer prod;
	private final MessageConsumer cons;

	public AMQBiQueue(String brokerUrl, String outName, String inName, Consumer<String> onMessage) throws JMSException {
		this(createSess(brokerUrl), outName, inName, onMessage);
	}

	private static Session createSess(String brokerUrl) throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("failover:(" + brokerUrl + ")?maxReconnectDelay=3000");
		Connection con = connectionFactory.createConnection();
		con.start();
		return con.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}
	
	public AMQBiQueue(Session sess, String outName, String inName, Consumer<String> onMessage) throws JMSException {
		this.sess = sess;
		
		outQueue = sess.createQueue(outName);
		inQueue = inName == null ? sess.createTemporaryQueue() : sess.createQueue(inName);
		
		prod = sess.createProducer(outQueue);
		cons = sess.createConsumer(inQueue);
		
		cons.setMessageListener(msg -> {
			try {
				onMessage.accept(((TextMessage)msg).getText());
			} catch (Throwable e) {
				log.warn("Error in onMessage", e);
			}
		});
		
	}
	
	public void sendMessage(String msg) {
		try {
			TextMessage tmsg = sess.createTextMessage();
			tmsg.setJMSReplyTo(inQueue);
			tmsg.setText(msg);
			prod.send(tmsg);
		} catch (JMSException e) {
//			throw Exceptions.wrap(e);
			throw new RuntimeException();
		}
	}
	
}
