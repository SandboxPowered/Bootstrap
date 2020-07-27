package org.sandboxpowered.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SandboxBootstrap {
    public static final String UPDATE_CHECK_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/maven-metadata.xml";
    public static final String DOWNLOAD_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric";
    public static final Logger LOG = LogManager.getLogger("Sandbox|Bootstrap");
}
