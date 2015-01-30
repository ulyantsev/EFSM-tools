package qbf.egorov.transducer.algorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {
	public static int hash(String str) {
		int hash = 0xAAAAAAAA;

		for (int i = 0; i < str.length(); i++) {
			if ((i & 1) == 0) {
				hash ^= ((hash << 7) ^ str.charAt(i) * (hash >> 3));
			} else {
				hash ^= (~((hash << 11) + str.charAt(i) ^ (hash >> 5)));
			}
		}

		return hash;
	}
	
	public static long RSHash(String str) {
		int b = 378551;
		int a = 63689;
		long hash = 0;

		for (int i = 0; i < str.length(); i++) {
			hash = hash * a + str.charAt(i);
			a = a * b;
		}

		return hash;
	}

	
	public static String md5(String str) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		byte[] array = digest.digest(str.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; ++i) {
			String m = Integer.toHexString((array[i] & 0xFF));
			if (m.length() == 1) {
				sb.append('0');
			}
			sb.append(m);
		}
		return sb.toString();
	}
}
