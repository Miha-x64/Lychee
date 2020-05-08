package android.util;


public class Base64 {

    public static final int DEFAULT = 0;

    public static final int NO_PADDING = 1;

    public static final int NO_WRAP = 2;

    public static final int CRLF = 4;

    public static final int URL_SAFE = 8;

    public static final int NO_CLOSE = 16;

    public static byte[] decode(String str, int flags) {
        return com.migcomponents.migbase64.Base64.decode(str);
    }

    public static byte[] decode(byte[] input, int flags) {
        throw new UnsupportedOperationException();
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        throw new UnsupportedOperationException();
    }

    public static String encodeToString(byte[] input, int flags) {
        return com.migcomponents.migbase64.Base64.encodeToString(input, false);
    }

    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        throw new UnsupportedOperationException();
    }

    public static byte[] encode(byte[] input, int flags) {
        throw new UnsupportedOperationException();
    }

    public static byte[] encode(byte[] input, int offset, int len, int flags) {
        throw new UnsupportedOperationException();
    }

    private Base64() { }   // don't instantiate
}
