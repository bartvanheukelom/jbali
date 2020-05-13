package org.jbali.serialize;

import kotlinx.serialization.json.JsonElement;
import org.apache.commons.codec.binary.Base64;
import org.jbali.collect.Maps;
import org.jbali.json.JSONArray;
import org.jbali.json.JSONObject;
import org.jbali.json.JsonConvertKt;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

/**
 * <p>Provides serialization of Java values from/to JSON, without loss of data or type information. Exceptions:</p>
 * <ul>
 * <li>All maps are unserialized as ImmutableMap NOPE NOT YET</li>
 * <li>All lists are unserialized as ImmutableList NOPE NOT YET</li>
 * </ul>
 * <p>Cyclic structures are not supported.</p>
 *
 * TODO use Kotlin serialization if available
 */
public class JavaJsonSerializer {
	
	enum ValType {
		
		JAVA_OBJECT('J'),
		JSON_ELEMENT('E'),

		BYTE('x'),
		CHAR('c'),
		INT('i'),
		SHORT('s'),
		LONG('l'),
		FLOAT('f'),

		BYTE_ARRAY('B'),
		
		
		// TODO these:
//		MAYBE('M'),
//		STRING_OBJECT('O'),
//		ARRAY('A'),
//		LIST('L'),
//		SET('T'),
//		MAP('M')
		;
		
		public final char letter;
		private ValType(char letter) {
			this.letter = letter;
		}
		
		private static final Map<Character, ValType> byLetter = Maps.mapCollection(Arrays.asList(values()), vt -> vt.letter);

		public static ValType getByLetter(char letter) {
			return byLetter.get(letter);
		}
		
	}
	
	private static JSONArray complex(ValType vt, Object inner) {
		return JSONArray.create(""+vt.letter, inner);
	}

	/**
	 * @return One of: <code>null</code>, Boolean, Double, String, JSONArray.
	 */
	public static Object serialize(Object val) {
		if (val == null || val instanceof Boolean || val instanceof String || val instanceof Double) return val;
		if (val instanceof JsonElement) return complex(ValType.JSON_ELEMENT, JsonConvertKt.convertToLegacy((JsonElement) val));
		if (val instanceof byte[]) return complex(ValType.BYTE_ARRAY, Base64.encodeBase64String((byte[]) val));
//		if (val instanceof Maybe<?>) return complex(ValType.MAYBE, serialize(((Maybe<?>) val).orNull()));
		if (val instanceof Character) return complex(ValType.CHAR, String.valueOf(val));
		if (val instanceof Number) {
			Number vn = (Number) val;
			if (val instanceof Short) return complex(ValType.SHORT, vn.doubleValue());
			if (val instanceof Byte) return complex(ValType.BYTE, vn.doubleValue());
			if (val instanceof Float) return complex(ValType.FLOAT, vn.doubleValue());
			if (val instanceof Integer) return complex(ValType.INT, vn.doubleValue());
			if (val instanceof Long) return complex(ValType.LONG, String.valueOf(vn));
		}
		if (val instanceof Serializable) return complex(ValType.JAVA_OBJECT, Base64.encodeBase64String(JavaSerializer.write((Serializable) val)));
		throw new IllegalArgumentException("Cannot serialize value " + val + " of type " + val.getClass());
	}

	/**
	 * @param json One of: <code>null</code>, <code>JSONObject.NULL</code>, Boolean, Number, String, JSONArray.
	 */
	public static Object unserialize(Object json) {
		if (json == JSONObject.NULL) return null;
		if (json == null || json instanceof Boolean || json instanceof String) return json;
		if (json instanceof Number) return ((Number) json).doubleValue();
		if (json instanceof JSONArray) {
			JSONArray ja = (JSONArray) json;
			char letter = ja.getString(0).charAt(0);
			ValType vt = ValType.getByLetter(letter);
			switch (vt) {
				case JSON_ELEMENT: return JsonConvertKt.jsonElementFromLegacy(ja.get(1));
				case JAVA_OBJECT: return JavaSerializer.read(Base64.decodeBase64(ja.getString(1)));				
				case BYTE_ARRAY: return Base64.decodeBase64(ja.getString(1));
				case BYTE: return ((Number)ja.get(1)).byteValue();
				case CHAR: return ja.getString(1).charAt(0);
				case FLOAT: return ((Number)ja.get(1)).floatValue();
				case INT: return ((Number)ja.get(1)).intValue();
				case LONG: return Long.parseLong(ja.getString(1));
				case SHORT: return ((Number)ja.get(1)).shortValue();
//				case MAYBE: return Maybe.fromNullable(unserialize(ja.get(1)));
			}
		}
		throw new IllegalArgumentException("Cannot unserialize " + JSONObject.valueToString(json));
	}
	
}
