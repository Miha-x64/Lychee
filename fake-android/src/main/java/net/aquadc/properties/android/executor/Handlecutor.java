package net.aquadc.properties.android.executor;

import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * Interface of {@code Handlecutor} class from {@code android-bindings} package.
 * Cannot be loaded out of Android, will cause {@link NoClassDefFoundError}.
 */
public final class Handlecutor implements Executor {

    static {
        if (true) {
            throw new NoClassDefFoundError("this class is for compile-time only");
        }
    }

    public Handlecutor(Looper looper) {
        throw new AssertionError();
    }

    @Override
    public void execute(Runnable command) {
        throw new AssertionError();
    }

}
