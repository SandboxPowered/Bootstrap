package org.sandboxpowered.bootstrap;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AutoUpdate {
    public static final String UPDATE_CHECK_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/maven-metadata.xml";
    public static final String DOWNLOAD_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/";
    private static final Logger LOG = LogManager.getLogger("Sandbox|Bootstrap");
    static JFrame frame;

    public static void doStuff() {
        @Nullable String headless = System.setProperty("java.awt.headless", "false");
        frame = new JFrame();
        BufferedImage image;
        try {
            image = ImageIO.read(AutoUpdate.class.getResource("/banner.png"));
        } catch (IOException e) {
            throw new RuntimeException("unable to read input stream", e);
        }
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, this);
            }
        };
        Icon imgIcon = new ImageIcon(AutoUpdate.class.getResource("/spinner.gif"));
        JLabel label = new JLabel(imgIcon);
        JLabel textLabel = new JLabel("A new update has been installed. Please restart your client", JLabel.CENTER);
        textLabel.setForeground(Color.white);
        label.setBounds(image.getWidth() / 2 - 50, image.getHeight() - 150, 100, 100);
        textLabel.setBounds(0, image.getHeight() - 150, image.getWidth(), 100);
        panel.setSize(image.getWidth(), image.getHeight());
        panel.setOpaque(false);
        frame.add(label);
        frame.add(textLabel);
        frame.add(panel);

        frame.setAlwaysOnTop(true);
        frame.setSize(image.getWidth(), image.getHeight());
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);
        frame.setUndecorated(true);
        textLabel.setVisible(false);
        frame.setVisible(true);
        AutoUpdate.Result result = check().join();
        if (result == AutoUpdate.Result.UPDATED_TO_LATEST) {
            label.setVisible(false);
            textLabel.setVisible(true);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(5480);
        }
        frame.setVisible(false);
        frame.dispose();
        if (headless != null) {
            System.setProperty("java.awt.headless", headless);
        }
    }

    public static CompletableFuture<Result> check() {
        LOG.info("Checking for updates");
        Path modsFolder = Paths.get("mods");
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

                if (v != null && (!v.equals(version) || !Files.exists(sandboxJar))) {
                    String url = String.format("%s%s/sandbox-fabric-%s.jar", DOWNLOAD_URL, v, v);
                    LOG.info("Downloading Sandbox v{}.", v);
                    try (InputStream inputStream = new URL(url).openStream()) {
                        Files.copy(inputStream, sandboxJar, StandardCopyOption.REPLACE_EXISTING);
                        Files.write(sandboxVersion, v.getBytes(StandardCharsets.UTF_8));
                        LOG.info("Downloaded Sandbox v{} please restart your client to apply changes.", v);
                        future.complete(Result.UPDATED_TO_LATEST);
                    } catch (IOException ex) {
                        LOG.error("Unable to download updates", ex);
                        future.complete(Result.UNABLE_TO_DOWNLOAD);
                    }
                } else {
                    LOG.info("Running latest Sandbox (v{})", v);
                    future.complete(Result.ON_LATEST);
                }
            } catch (IOException | SAXException | ParserConfigurationException e) {
                LOG.error("Unable to check for updates", e);
                future.complete(Result.UNABLE_TO_CHECK);
            }
        });
        return future;
    }

    public static String readStringFromURL(String requestURL) throws IOException {
        try (Scanner scanner = new Scanner(new URL(requestURL).openStream(), StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public enum Result {
        ON_LATEST,
        UPDATED_TO_LATEST,
        UNABLE_TO_DOWNLOAD,
        UNABLE_TO_CHECK
    }
}
