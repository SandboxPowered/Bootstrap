package org.sandboxpowered.bootstrap;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class AutoUpdate {
    public static final String UPDATE_CHECK_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/maven-metadata.xml";
    public static final String DOWNLOAD_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/";
    private static final Logger LOG = LogManager.getLogger("Sandbox|Bootstrap");

    public static void updateServer() {
        IntConsumer updateProgress = i -> {
            String progress = Strings.repeat("=", i / 10);
            String left = Strings.repeat(" ", 10 - progress.length());
            System.out.print("\r[" + progress + left + "] " + i + "%");
        };

        BooleanConsumer showProgress = b -> {
            if (b) System.out.print("[          ] 0%");
            else System.out.println();
        };

        if (AutoUpdate.check(updateProgress, showProgress, LOG::info, LOG::error) == AutoUpdate.Result.UPDATED_TO_LATEST) {
            LOG.info("A new update has been installed. Please restart your server to apply changes");
            System.exit(5480);
        }
    }

    public static void updateClient() {
        @Nullable String headless = System.setProperty("java.awt.headless", "false");
        JFrame frame = new JFrame();
        //TODO add icon.png
        //frame.setIconImage(ImageIO.read(AutoUpdate.class.getResourceAsStream("/icon.png")));
        BufferedImage image;
        try {
            image = ImageIO.read(AutoUpdate.class.getResource("/banner.png"));
        } catch (IOException e) {
            throw new RuntimeException("unable to read input stream", e);
        }

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                graphics.drawImage(image, 0, 0, this);
            }
        };
        Icon imgIcon = new ImageIcon(AutoUpdate.class.getResource("/loading.gif"));
        JLabel label = new JLabel(imgIcon);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(image.getWidth() - 300, 50));
        progressBar.setUI(new DownloadProgressUI());
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setForeground(Color.RED);
        JLabel textLabel = new JLabel("", JLabel.CENTER);
        textLabel.setForeground(Color.WHITE);
        label.setBounds(image.getWidth() / 2 - 50, image.getHeight() - 150, 100, 100);
        textLabel.setBounds(0, image.getHeight() - 150, image.getWidth(), 100);

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cons = new GridBagConstraints();
        cons.anchor = GridBagConstraints.SOUTH;
        cons.weighty = 1;
        layout.addLayoutComponent(progressBar, cons);
        panel.setLayout(layout);

        panel.setSize(image.getWidth(), image.getHeight());
        panel.setOpaque(false);
        progressBar.setVisible(false);
        panel.add(progressBar);
        frame.add(label);
        frame.add(textLabel);
        frame.add(panel);

        //frame.setAlwaysOnTop(true);
        frame.setSize(image.getWidth(), image.getHeight());
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);
        frame.setUndecorated(true);
        textLabel.setVisible(false);
        frame.setVisible(true);

        IntConsumer updateProgress = i -> SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(i);
                    progressBar.setString(i + "%");
                });

        Consumer<String> info = s -> {
            LOG.info(s);
            textLabel.setText(s);
            label.setVisible(false);
            textLabel.setVisible(true);
        };

        BiConsumer<String, Exception> error = (s, e) -> {
            LOG.error(s, e);
            textLabel.setText("Error: " + s + " - " + e.getMessage());
            label.setVisible(false);
            textLabel.setVisible(true);
        };
        
        if (AutoUpdate.check(updateProgress, progressBar::setVisible, info, error) == AutoUpdate.Result.UPDATED_TO_LATEST) {
            textLabel.setText("A new update has been installed. Please restart your client to apply changes");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
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

    //This could be moved from functional programming to object oriented
    public static Result check(IntConsumer progressSetter, BooleanConsumer showProgress, Consumer<String> infoLogger, BiConsumer<String, Exception> errorLogger) {
        infoLogger.accept("Checking for updates");
        Path home;
        if (SystemUtils.IS_OS_WINDOWS) {
            home = Paths.get(System.getenv("APPDATA"), ".sandbox");
        } else if (SystemUtils.IS_OS_MAC) {
            home = Paths.get(System.getProperty("user.home"), "Local Settings\\ApplicationData\\.sandbox");
        } else if (SystemUtils.IS_OS_LINUX) {
            home = Paths.get(System.getProperty("user.home"), ".sandbox");
        } else {
            throw new IllegalArgumentException("Unsupported OS");
        }

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

        String version = currentVersion;
        try {
            String s = readStringFromURL(UPDATE_CHECK_URL);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));

            Element element = doc.getDocumentElement();

            NodeList e = element.getElementsByTagName("latest").item(0).getChildNodes();

            String v = e.item(0).getNodeValue();

            boolean jarExists = Files.exists(sandboxJar);
            if (v != null && (!v.equals(version) || !jarExists)) {
                String url = String.format("%s%s/sandbox-fabric-%s.jar", DOWNLOAD_URL, v, v);
                try {
                    infoLogger.accept("Downloading Sandbox v" + v + ".");
                    if (!Files.exists(home)) Files.createDirectory(home);
                    //Open connection and get download size
                    HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
                    long completeFileSize = httpConnection.getContentLength();

                    //Download data to temp file
                    Path temp = home.resolve("sandbox-" + v + ".jar");
                    if (Files.exists(temp)) {
                        progressSetter.accept(100);
                    } else {
                        Files.createFile(temp);
                        showProgress.accept(true);
                        BufferedInputStream input = new BufferedInputStream(httpConnection.getInputStream());
                        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(temp.toFile()), 1024);
                        byte[] data = new byte[1024];
                        long downloadedFileSize = 0;
                        int x;
                        while ((x = input.read(data, 0, 1024)) >= 0) {
                            //Add data and update progress bar
                            downloadedFileSize += x;
                            progressSetter.accept((int) (downloadedFileSize / (double) completeFileSize * 100.0));
                            output.write(data, 0, x);
                        }

                        //Close connections
                        input.close();
                        output.close();
                        showProgress.accept(false);
                    }
                    if (jarExists) Files.delete(sandboxJar);
                    Files.copy(temp, sandboxJar);
                    Files.write(sandboxVersion, v.getBytes(StandardCharsets.UTF_8));
                    infoLogger.accept(String.format("Downloaded Sandbox v%s", v));
                    return Result.UPDATED_TO_LATEST;
                } catch (IOException ex) {
                    errorLogger.accept("Unable to download updates", ex);
                    return Result.UNABLE_TO_DOWNLOAD;
                }
            } else {
                infoLogger.accept("Running latest " + v);
                return Result.ON_LATEST;
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            errorLogger.accept("Unable to check for updates", e);
            return Result.UNABLE_TO_CHECK;
        }
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

    private static class DownloadProgressUI extends BasicProgressBarUI {
        @Override
        protected void paintIndeterminate(Graphics g, JComponent c) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle r = getBox(new Rectangle());
            g.setColor(progressBar.getForeground());
            g.fillOval(r.x, r.y, r.width, r.height);
        }
    }
}