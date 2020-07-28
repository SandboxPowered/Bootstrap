package org.sandboxpowered.bootstrap.util;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import org.sandboxpowered.bootstrap.AutoUpdate;
import org.sandboxpowered.bootstrap.SandboxBootstrap;
import org.sandboxpowered.bootstrap.util.download.DownloadManager;
import org.sandboxpowered.bootstrap.util.download.DownloadResult;
import org.sandboxpowered.bootstrap.util.download.ProgressCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class SandboxUpdateChecker {

    public static AutoUpdate.Result check(ProgressCallback progressCallback) {
        SandboxBootstrap.LOG.info("Checking for updates");

        Path modsFolder = FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("mods");
        Path sandboxVersion = modsFolder.resolve("sandbox.version");
        Path sandboxJar = modsFolder.resolve("sandbox.jar");
        String currentVersion = null;
        if (Files.exists(sandboxVersion)) {
            try (InputStream stream = Files.newInputStream(sandboxVersion, StandardOpenOption.READ)) {
                currentVersion = IOUtils.toString(stream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("unable to read existing Sandbox version", e);
            }
        }

        Document doc;
        try {
            String s = DownloadManager.readStringFromURL(SandboxBootstrap.UPDATE_CHECK_URL);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
        } catch (SAXException | ParserConfigurationException | IOException e) {
            SandboxBootstrap.LOG.error("unable to check for latest sandbox version", e);
            return AutoUpdate.Result.UNABLE_TO_CHECK;
        }
        Element element = doc.getDocumentElement();
        NodeList nodes = element.getElementsByTagName("latest").item(0).getChildNodes();
        String v = nodes.item(0).getNodeValue();

        if (v != null && (!v.equals(currentVersion) || !Files.exists(sandboxJar))) {
            String url = String.format("%s/%s/sandbox-%s-%s.jar", SandboxBootstrap.DOWNLOAD_URL, v, Edition.FABRIC.getPrefix(), v);
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
                Files.copy(cachedJar, sandboxJar, StandardCopyOption.REPLACE_EXISTING);
                Files.write(sandboxVersion, v.getBytes(StandardCharsets.UTF_8));
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
