package org.sandboxpowered.bootstrap;

import com.google.common.base.MoreObjects;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

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
    public static final String MINECRAFT_VERSION_MANIFEST_URL = System.getProperty("sandbox.bootstrap.custom_minecraft_version_manifest", "https://launchermeta.mojang.com/mc/game/version_manifest.json");
    public static final String SANDBOX_FABRIC_VERSION_MANIFEST_URL = System.getProperty("sandbox.bootstrap.custom_sandbox_version_manifest", "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/maven-metadata.xml");
    public static final String SANDBOX_FABRIC_MAVEN_URL = System.getProperty("sandbox.bootstrap.custom_sandbox_maven_url", "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric");

    /**
     * @deprecated use {@link org.sandboxpowered.bootstrap.util.SandboxFolder} instead
     */
    @Nullable
    @Deprecated
    public static final String CUSTOM_SANDBOX_FOLDER_LOCATION = System.getProperty("sandbox.cache_location");

    private static int getInt(String name, IntSupplier defaultValueGetter) {
        return MoreObjects.firstNonNull(Integer.getInteger(name), defaultValueGetter.getAsInt());
    }

    private static boolean operatingSystemSupportsGUI() {
        Util.OperatingSystem os = Util.getOperatingSystem();
        return os == Util.OperatingSystem.WINDOWS || os == Util.OperatingSystem.OSX;
    }
}
