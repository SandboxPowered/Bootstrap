package org.sandboxpowered.bootstrap.util;

import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.util.UrlConversionException;
import net.fabricmc.loader.util.UrlUtil;
import org.jetbrains.annotations.Nullable;
import org.sandboxpowered.bootstrap.AutoUpdate;
import org.sandboxpowered.bootstrap.SandboxBootstrap;
import org.sandboxpowered.bootstrap.util.download.DownloadManager;
import org.sandboxpowered.bootstrap.util.download.DownloadResult;
import org.sandboxpowered.bootstrap.util.callback.ProgressCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class SandboxUpdateChecker {

    public static AutoUpdate.Result check(ProgressCallback progressCallback) {
        SandboxBootstrap.LOG.info("Checking for updates");

        Path modsFolder = FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("mods");
        Path sandboxJar = modsFolder.resolve("sandbox.jar");


        Version currentVersion = FabricLoader.getInstance().getModContainer(SandboxBootstrap.SANDBOX_MODID).map(net.fabricmc.loader.api.ModContainer::getMetadata).map(ModMetadata::getVersion).orElse(null);

        Document doc;
        try {
            String s = DownloadManager.readStringFromURL(SandboxBootstrap.SANDBOX_FABRIC_VERSION_MANIFEST);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
        } catch (SAXException | ParserConfigurationException | IOException e) {
            SandboxBootstrap.LOG.error("unable to check for latest sandbox version", e);
            return AutoUpdate.Result.UNABLE_TO_CHECK;
        }
        Element element = doc.getDocumentElement();
        NodeList nodes = element.getElementsByTagName("latest").item(0).getChildNodes();
        @Nullable String v = nodes.item(0).getNodeValue();
        Version latest;
        try {
            if(v == null) {
                throw new VersionParsingException("null version");
            }
            latest = Version.parse(v);
        } catch (VersionParsingException e) {
            SandboxBootstrap.LOG.error("Unable to parse latest version", e);
            return AutoUpdate.Result.UNABLE_TO_CHECK;
        }

        if (!latest.equals(currentVersion) || !Files.exists(sandboxJar)) {
            String url = String.format("%s/%s/sandbox-%s-%s.jar", SandboxBootstrap.SANDBOX_FABRIC_DOWNLOAD_URL, v, Edition.FABRIC.getPrefix(), v);
            SandboxBootstrap.LOG.info("Downloading Sandbox v" + v + "...");
            Path cachedJar = SandboxFolder.getSandboxJar(Edition.FABRIC, v);
            try { //TODO move to proper download manager impl
                if (!Files.exists(cachedJar)) { // download only if we don't have a cached version
                    CompletableFuture<DownloadResult> download = DownloadManager.download(url, cachedJar, "sandbox-fabric-" + v, progressCallback);

                    //do other stuff here?

                    DownloadResult result = download.join();
                    if (result.getResult() != DownloadResult.Type.SUCCESS) {
                        SandboxBootstrap.LOG.error("unable to download - {}", result.getResult().name());
                        if (result.getMessage() != null) {
                            SandboxBootstrap.LOG.error(result.getMessage(), result.getError());
                        }
                        return AutoUpdate.Result.UNABLE_TO_DOWNLOAD;
                    }
                }
                if (Files.exists(sandboxJar)) {
                    SandboxBootstrap.LOG.info("Removing existing Sandbox files...");
                    SandboxBootstrap.LOG.debug("Attempting to close loaded file systems");
                    Iterator<ModContainer> it = FabricLoader.getInstance().getAllMods().stream().map(ModContainer.class::cast).iterator();
                    while (it.hasNext()) {
                        ModContainer container = it.next();
                        String modid = container.getMetadata().getId();
                        if (!modid.equals(SandboxBootstrap.MOD_ID)) {
                            try {
                                Path holder = UrlUtil.asPath(container.getOriginUrl()).toAbsolutePath();
                                URI pathUri = holder.toUri();
                                if (!Files.isDirectory(holder)) {
                                    pathUri = new URI("jar:" + pathUri.getScheme(), pathUri.getHost(), pathUri.getPath(), pathUri.getFragment());
                                }
                                SandboxBootstrap.LOG.trace("Closing file system for mod {}", modid);
                                IOUtil.close(FileSystems.getFileSystem(pathUri));
                            } catch (UrlConversionException | IOException | URISyntaxException e) {
                                SandboxBootstrap.LOG.error("Unable to close mod file systems", e);
                                return AutoUpdate.Result.UNABLE_TO_DOWNLOAD;
                            }
                        }
                    }
                    SandboxBootstrap.LOG.warn("Attempting to close Knot's parent ClassLoader, this may cause issues");
                    if(!IOUtil.tryClose(FabricLauncherBase.getLauncher().getTargetClassLoader().getParent())) {
                        SandboxBootstrap.LOG.error("Unable to close ClassLoader: does not implement AutoCloseable!");
                        return AutoUpdate.Result.UNABLE_TO_DOWNLOAD;
                    }
                }
                Files.copy(cachedJar, sandboxJar, StandardCopyOption.REPLACE_EXISTING);
                SandboxBootstrap.LOG.info("Downloaded Sandbox v" + v);
                return AutoUpdate.Result.UPDATED_TO_LATEST;
            } catch (IOException e) {
                SandboxBootstrap.LOG.error("unable to get new sandbox version", e);
            }
            return AutoUpdate.Result.UNABLE_TO_DOWNLOAD;
        } else {
            SandboxBootstrap.LOG.info("Running latest Sandbox (v" + v + ")");
            return AutoUpdate.Result.ON_LATEST;
        }
    }
}
