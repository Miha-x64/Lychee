package android.os;

/**
 * Pretends to be {@link android.os.Handler}.
 * Used as a {@code compileOnly} dependency to avoid reflection.
 */
public class Handler {

    static {
        if (true) {
            throw new NoClassDefFoundError("this class in for compile-time only");
        }
    }

    public Handler(Looper looper) {
        throw null;
    }

    public final boolean post(Runnable r) {
        throw null;
    }

    public final boolean postDelayed(Runnable r, long delayMillis) {
        throw null;
    }

}
