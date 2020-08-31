package org.sandboxpowered.bootstrap;

import com.google.common.base.MoreObjects;
import net.minecraft.util.Util;

import java.util.function.IntSupplier;

public class Constants {

    /**
     * the maximum amount of downloads that should be running in parallel
     */
    public static final int MAX_CONNECTIONS = Math.max(1, getInt("sandbox.bootstrap.max_parallel_connections", () -> Runtime.getRuntime().availableProcessors() - 1));
    /**
     * the size of each connection's download buffer, in {@code bytes}
     */
    public static final int DOWNLOAD_BUFFER_SIZE = getInt("sandbox.bootstrap.download_buffer_size", () -> 1024);

    public static final boolean FORCE_HEADLESS = Boolean.getBoolean("sandbox.bootstrap.headless") || !operatingSystemSupportsGUI();

    private static int getInt(String name, IntSupplier defaultValueGetter) {
        return MoreObjects.firstNonNull(Integer.getInteger(name), defaultValueGetter.getAsInt());
    }

    private static boolean operatingSystemSupportsGUI() {
        Util.OperatingSystem os = Util.getOperatingSystem();
        return os == Util.OperatingSystem.WINDOWS || os == Util.OperatingSystem.OSX;
    }
}
