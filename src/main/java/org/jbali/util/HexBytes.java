package org.jbali.util;

import org.jetbrains.annotations.NotNull;

public class HexBytes {

	public static String toAscii(byte[] dd, int pad) {
		int w = pad+1;
		char[] s = new char[dd.length*w];
		for (int i = 0; i < dd.length; i++) {
			byte b = dd[i];
			char c = b >= 32 && b < 127 ? (char) b : '\u2022';
			s[i*w] = c;
			for (int p = 0; p < pad; p++)
				s[i*w+p] = ' ';
		}
		return new String(s);
	}
	
	/**
	 * @return The bytes in hex format (uppercase)
	 */
	public static @NotNull String toHex(@NotNull byte[] msgB) {
		return toHex(msgB, msgB.length);
	}
	
	public static @NotNull byte[] parseHex(@NotNull String hex) {
		byte[] b = new byte[hex.length() / 2];
		for (int i = 0; i < b.length; i++) {
			String hexByte = hex.substring(i * 2, i * 2 + 2);
			b[i] = (byte) (Integer.parseInt(hexByte, 16) & 0xFF);
		}
		return b;
	}

	public static @NotNull String toHex(@NotNull byte[] data, int limit) {
		return toHex(data, limit, true);
	}

	public static @NotNull String toHex(@NotNull byte[] data, int limit, boolean upperCase) {
		StringBuilder b = new StringBuilder(data.length*2);
		int i = 0;
		for (byte bt : data) {
			if (i == limit) break;
			String h = Integer.toHexString(bt & 0xFF);
			if (h.length() == 1) b.append('0');
			b.append(upperCase ? h.toUpperCase() : h);
			i++;
		}
		if (data.length > limit) {
			b.append("... (+");
			b.append(data.length-limit);
			b.append(")");
		}
		return b.toString();
		
	}
	
}
