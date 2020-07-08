package android.util;

public final class Base64 {

    /*public static final int DEFAULT = 0;

    public static final int NO_PADDING = 1;

    public static final int NO_WRAP = 2;

    public static final int CRLF = 4;

    public static final int URL_SAFE = 8;

    public static final int NO_CLOSE = 16;*/

    public static byte[] decode(String str, int flags) {
        checkFlags(flags);
        return java.util.Base64.getDecoder().decode(str);
    }

    /*public static byte[] decode(byte[] input, int flags) {
        throw new UnsupportedOperationException();
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        throw new UnsupportedOperationException();
    }*/

    public static String encodeToString(byte[] input, int flags) {
        checkFlags(flags);
        return java.util.Base64.getEncoder().encodeToString(input);
    }

    /*public static String encodeToString(byte[] input, int offset, int len, int flags) {
        throw new UnsupportedOperationException();
    }

    public static byte[] encode(byte[] input, int flags) {
        throw new UnsupportedOperationException();
    }

    public static byte[] encode(byte[] input, int offset, int len, int flags) {
        throw new UnsupportedOperationException();
    }*/

    private static void checkFlags(int flags) {
        if (flags != 0) {
            throw new IllegalArgumentException("Unexpected flags: 0b" + Integer.toBinaryString(flags) +
                    ". Note this I'm not real android.util.Base64 but Lychee:android-json-on-jvm");
        }
    }

    private Base64() { }   // don't instantiate
}