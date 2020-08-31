package org.sandboxpowered.bootstrap.util;

import java.io.IOException;
import java.util.Arrays;

public class IOUtil {

    public static boolean tryClose(Object... toClose) throws IOException {
        AutoCloseable[] array = Arrays.stream(toClose).filter(AutoCloseable.class::isInstance).map(AutoCloseable.class::cast).toArray(AutoCloseable[]::new);
        if (array.length == 0) {
            return false;
        }
        close(array);
        return true;
    }

    public static void close(AutoCloseable... toClose) throws IOException {
        IOException parent = null;
        for (AutoCloseable closeable : toClose) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    if (parent == null) {
                        parent = new IOException("Unable to close one or more objects");
                    }
                    parent.addSuppressed(e);
                }
            }
        }
        if (parent != null) throw parent;
    }
}
