package org.jbali.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class JavaSerializer {//implements Serializer {

	public static byte[] write(Object message) {
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(b);
			out.writeObject(message);
			out.flush();
			return b.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object read(byte[] data) {
		try {
			ByteArrayInputStream b = new ByteArrayInputStream(data);
			ObjectInputStream in = new ObjectInputStream(b);
			return in.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * @return A copy of the given object created using Java serialization
	 */
	public static <T> T copy(T obj) {
		//noinspection unchecked
		return (T) read(write(obj));
	}
	
//	@Override
//	public byte[] serialize(Object message) {
//		return write(message);
//	}
//
//	@Override
//	public Object unserialize(byte[] data) {
//		return read(data);
//	}

}
