package org.sandboxpowered.bootstrap.util;

import com.google.common.base.MoreObjects;
import net.minecraft.util.Util;
import org.sandboxpowered.bootstrap.Constants;
import org.sandboxpowered.bootstrap.SandboxBootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SandboxFolder {
    private static final Path ROOT;
    private static final Path DOWNLOAD_DIRECTORY;
    private static final Path SANDBOX_JAR_DIRECTORY;
    private static final Path ADDONS_DIRECTORY;


    static {
        //noinspection deprecation
        if(Constants.CUSTOM_SANDBOX_FOLDER_LOCATION != null) {
            //noinspection deprecation
            ROOT = Paths.get(Constants.CUSTOM_SANDBOX_FOLDER_LOCATION);
        }
        else {
            String homeStr = System.getProperty("user.home", ".");
            Path userHome;
            switch (Util.getOperatingSystem()) {
                case WINDOWS:
                    String appData = System.getenv("APPDATA");
                    //Use home directory for windows instead of crashing if appdata doesn't exist
                    userHome = Paths.get(MoreObjects.firstNonNull(appData, homeStr));
                    break;
                case OSX:
                    userHome = Paths.get(homeStr, "Library", "Application Support");
                    break;
                default:
                    userHome = Paths.get(homeStr);
                    break;
            }
            ROOT = userHome.resolve(".sandbox");
        }

        DOWNLOAD_DIRECTORY = ROOT.resolve("downloads");
        SANDBOX_JAR_DIRECTORY = ROOT.resolve("versions");
        ADDONS_DIRECTORY = ROOT.resolve("addons");

        //clean up old, partial downloads
        if (Files.exists(DOWNLOAD_DIRECTORY)) {
            try {
                //TODO find better way to check if dir not empty
                boolean delete;
                try (Stream<Path> test = Files.list(DOWNLOAD_DIRECTORY)) {
                    delete = test.findAny().isPresent();
                }
                if (delete) {
                    SandboxBootstrap.LOG.info("Clearing previous download cache.");
                    FileUtils.deleteDirectory(DOWNLOAD_DIRECTORY, false);
                }
            } catch (IOException e) {
                SandboxBootstrap.LOG.warn("Unable to clear old download cache!", e);
            }
        }
    }

    public static Path getPath() {
        return FileUtils.mkdir(ROOT);
    }

    public static Path getAddonsPath() {
        return FileUtils.mkdir(ADDONS_DIRECTORY);
    }

    public static Path getDownloadPath() {
        return FileUtils.mkdir(DOWNLOAD_DIRECTORY);
    }

    public static Path getSandboxJar(Edition edition, String version) {
        return FileUtils.mkdirParent(SANDBOX_JAR_DIRECTORY.resolve(String.format("sandbox-%s-%s.jar", edition.getPrefix(), version)));
    }
}
