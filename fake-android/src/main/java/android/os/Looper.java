package android.os;

/**
 * Pretends to be {@link android.os.Looper}.
 * Used as a {@code compileOnly} dependency to avoid reflection.
 */
public final class Looper {

    static {
        if (true) {
            throw new NoClassDefFoundError("this class in for compile-time only");
        }
    }

    public static Looper myLooper() {
        throw null;
    }

}
