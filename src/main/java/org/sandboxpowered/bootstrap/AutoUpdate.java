package org.sandboxpowered.bootstrap;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.sandboxpowered.bootstrap.gui.GuiDownloadHandler;
import org.sandboxpowered.bootstrap.util.SandboxUpdateChecker;
import org.sandboxpowered.bootstrap.util.callback.CloseCallback;
import org.sandboxpowered.bootstrap.util.callback.PostDownloadCallback;
import org.sandboxpowered.bootstrap.util.callback.PreDownloadCallback;
import org.sandboxpowered.bootstrap.util.callback.ProgressCallback;
import org.sandboxpowered.bootstrap.util.download.DelegatingDownloadHandler;
import org.sandboxpowered.bootstrap.util.download.DownloadHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class AutoUpdate {

    public static final ProgressCallback HEADLESS_PROGRESS_CALLBACK = (bytesDownloaded, bytesTotal, stage) -> {
        switch (stage) {
            case PREPARING:
                System.out.print("[          ] 0%");
                break;
            case DOWNLOADING:
                int percentage = (int) (bytesDownloaded / (double) bytesTotal * 100.0);
                if (percentage > 0) {
                    String progress = Strings.repeat("=", percentage / 10);
                    String left = Strings.repeat(" ", 10 - progress.length());
                    System.out.print("\r[" + progress + left + "] " + percentage + "%");
                }
                break;
            case COPYING:
                System.out.print("\r[==========] 100% - cleaning up");
                break;
            case FINISHED:
                System.out.println("\r[==========] 100% - done!");
                break;
        }
    };

    private static void updateServer() {
        if (SandboxUpdateChecker.check(HEADLESS_PROGRESS_CALLBACK) == AutoUpdate.Result.UPDATED_TO_LATEST) {
            SandboxBootstrap.LOG.info("A new update has been installed. Please restart your server to apply changes");
            System.exit(5480);
        }
    }

    static CloseCallback closeCallback = () -> {};

    private static DownloadHandler createHeadlessDownloadHandler() {
        //TODO populate stub methods
        PreDownloadCallback pre = () -> {};
        PostDownloadCallback post = () -> {};
        CloseCallback close = () -> {};
        return DelegatingDownloadHandler.of(HEADLESS_PROGRESS_CALLBACK, pre, post, close);
    }

    private static void updateClient() {
            DownloadHandler downloadHandler = Constants.FORCE_HEADLESS ? createHeadlessDownloadHandler() : new GuiDownloadHandler();
            closeCallback = downloadHandler;
        Result result = SandboxUpdateChecker.check(downloadHandler);
        if (result == Result.UPDATED_TO_LATEST) {
            downloadHandler.onFinishedDownloading();
            closeCallback.onClose();
            System.exit(5480);
        }
    }

    public static void closeClientWindow() {
        closeCallback.onClose();
    }

    public static void onLaunch() {
        try (Reader reader = new InputStreamReader(new URL(Constants.MINECRAFT_VERSION_MANIFEST_URL).openStream())) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            Version latest = Version.parse(json.getAsJsonObject("latest").get("release").getAsString());
            Version current = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow(() -> new IllegalStateException("minecraft not found")).getMetadata().getVersion();
            if(!current.equals(latest)) {
                SandboxBootstrap.LOG.warn("=============================================================================");
                SandboxBootstrap.LOG.warn("Outdated Minecraft Version detected! (you are on {}, latest is {})", current::getFriendlyString, latest::getFriendlyString);
                SandboxBootstrap.LOG.warn("This is not supported. Sandbox Bootstrap will shut down.");
                SandboxBootstrap.LOG.warn("=============================================================================");
                return;
            }
        } catch (JsonParseException | IOException | VersionParsingException e) {
            SandboxBootstrap.LOG.error("Unable to check for Sandbox updates", e);
            return;
        }

        //all checks passed, continue loading
        switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT:
                updateClient();
                break;
            case SERVER:
                updateServer();
                break;
        }
    }

    public enum Result {
        ON_LATEST,
        UPDATED_TO_LATEST,
        UNABLE_TO_DOWNLOAD,
        UNABLE_TO_CHECK
    }
}
