package org.sandboxpowered.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SandboxBootstrap {
    public static final String MODID = "sandbox_bootstrap";
    public static final String MINECRAFT_VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String SANDBOX_FABRIC_VERSION_MANIFEST = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/maven-metadata.xml";
    public static final String SANDBOX_FABRIC_DOWNLOAD_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric";
    public static final Logger LOG = LogManager.getLogger("Sandbox|Bootstrap");

    public static void main(String[] args) {
        // TODO add installer GUI
        throw new UnsupportedOperationException("Standalone installer functionality not implemented yet!");
    }
}
