package org.sandboxpowered.bootstrap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AutoUpdate {
    private static final Logger LOG = LogManager.getLogger("Sandbox|Bootstrap");
    public static final String UPDATE_CHECK_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/maven-metadata.xml";
    public static final String DOWNLOAD_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/";

    public static enum Result {
        ON_LATEST,
        UPDATED_TO_LATEST,
        UNABLE_TO_DOWNLOAD,
        UNABLE_TO_CHECK
    }

    public static String readStringFromURL(String requestURL) throws IOException {
        try (Scanner scanner = new Scanner(new URL(requestURL).openStream(), StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public static CompletableFuture<Result> check() throws IOException {
        LOG.info("Checking for updates");
        File currentFile = new File(".");
        File modsFolder = new File(currentFile, "mods");
        File sandboxVersion = new File(modsFolder, "sandbox.version");
        File sandboxJar = new File(modsFolder, "sandbox.jar");

        String currentVersion = null;

        if (sandboxVersion.exists()) {
            currentVersion = FileUtils.readFileToString(sandboxVersion, StandardCharsets.UTF_8);
        }

        CompletableFuture<Result> future = new CompletableFuture<>();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        String version = currentVersion;
        executorService.submit(() -> {
            try {
                String s = readStringFromURL(UPDATE_CHECK_URL);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));

                Element element = doc.getDocumentElement();

                NodeList e = element.getElementsByTagName("latest").item(0).getChildNodes();

                String v = e.item(0).getNodeValue();

                if (v == null || !v.equals(version) || !sandboxJar.exists()) {
                    String url = String.format("%s%s/sandbox-fabric-%s.jar", DOWNLOAD_URL, v, v);
                    if (sandboxJar.exists())
                        sandboxJar.delete();

                    try {
                        LOG.info("Downloading Sandbox v'{}'.", v);
                        FileUtils.copyURLToFile(new URL(url), sandboxJar);
                        FileUtils.writeStringToFile(sandboxVersion, v, StandardCharsets.UTF_8);
                        LOG.info("Downloaded Sandbox v'{}' please restart your client to apply changes.", v);
                        future.complete(Result.UPDATED_TO_LATEST);
                    } catch (IOException ex) {
                        LOG.error("Unable to download updates", ex);
                        future.complete(Result.UNABLE_TO_DOWNLOAD);
                    }
                } else {
                    LOG.info("Running latest '{}'", v);
                    future.complete(Result.ON_LATEST);
                }
            } catch (IOException | SAXException | ParserConfigurationException e) {
                LOG.error("Unable to check for updates", e);
                future.complete(Result.UNABLE_TO_CHECK);
            }
        });
        return future;
    }
}