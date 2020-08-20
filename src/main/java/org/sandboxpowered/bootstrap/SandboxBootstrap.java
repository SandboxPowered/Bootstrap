package org.sandboxpowered.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.sandboxpowered.bootstrap.util.SandboxUpdateChecker;
import org.sandboxpowered.bootstrap.util.download.ProgressCallback;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SandboxBootstrap {
    public static final String MOD_ID = "sandbox_bootstrap";
    public static final String SANDBOX_MODID = "sandbox";
    public static final String MINECRAFT_VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String SANDBOX_FABRIC_VERSION_MANIFEST = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric/maven-metadata.xml";
    public static final String SANDBOX_FABRIC_DOWNLOAD_URL = "https://dl.bintray.com/sandboxpowered/Loader/org/sandboxpowered/sandbox-fabric";
    public static final Logger LOG = LogManager.getLogger("Sandbox|Bootstrap");

    public static void main(String[] args) {
        // TODO add installer GUI

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            SandboxBootstrap.LOG.error("Unable to set Swing Look-And-Feel", e);
        }

        JFrame frame = new JFrame();
        BufferedImage image;
        try {
            image = ImageIO.read(AutoUpdate.class.getResource("/sboxbootstrap/banner.png"));
            frame.setIconImage(ImageIO.read(AutoUpdate.class.getResource("/sboxbootstrap/icon.png")));
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

        Icon imgIcon = new ImageIcon(AutoUpdate.class.getResource("/sboxbootstrap/loading.gif"));
        JLabel label = new JLabel(imgIcon);
        label.setBounds(image.getWidth() / 2 - 50, image.getHeight() - 150, 100, 100);

        panel.setSize(image.getWidth(), image.getHeight());
        panel.setOpaque(false);
        frame.add(label);
        frame.add(panel);

        frame.setSize(image.getWidth(), image.getHeight());
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);
        frame.setVisible(true);

        if (JOptionPane.showOptionDialog(frame, "Standalone installer functionality not implemented yet!\nYou can run this Jar as a Fabric mod to install Sandbox.", "Sandbox", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {
            System.exit(0);
        }
    }
}
