package pl.przemelek.android.util;

public class Util {
	private static String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	 
	public static String encodeToBase64(String text) {
		int[] encoded = new int[text.length()*8/6+4];
		int trueLen = text.length()*8/6+1;
		int newLen = 0;
		for (int idx=0; idx<text.length(); idx+=3) {
			int end = idx+3;
			if (end>text.length()) {
				end = text.length();
			}
			String toWork = text.substring(idx,end);
			char a = 0;
			char b = 0;
			char c = 0;
			if (toWork.length()>0) { a = toWork.charAt(0); }
			if (toWork.length()>1) { b = toWork.charAt(1); }
			if (toWork.length()>2) { c = toWork.charAt(2); }
			int _a = (a & (63 << 2)) >> 2;			
			int _b = ((a & 3)  << 4) +((b & (15 <<4)) >> 4);
			int _c = ((b & 15) << 2) +((c & (3  <<6)) >> 6);
			int _d = (c & 63);
			encoded[newLen++] = charSet.charAt(_a);
			encoded[newLen++] = charSet.charAt(_b);
			encoded[newLen++] = charSet.charAt(_c);
			encoded[newLen++] = charSet.charAt(_d);			
		}
		String encodedStr = "";
		for (int idx=0; idx<trueLen; idx++) {
			if (encoded[idx]!=0) {
				encodedStr+=(char)encoded[idx];
			}
		}
		while (encodedStr.length()%4!=0) {
			encodedStr+="=";
		}
		return encodedStr;
	}
}
