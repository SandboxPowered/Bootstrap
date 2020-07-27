package org.sandboxpowered.bootstrap;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Nullable;
import org.sandboxpowered.bootstrap.util.SandboxUpdateChecker;
import org.sandboxpowered.bootstrap.util.download.ProgressCallback;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AutoUpdate {

    public static void updateServer() {
        ProgressCallback progressCallback = (bytesDownloaded, bytesTotal, stage) -> {
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

        if (SandboxUpdateChecker.check(progressCallback) == AutoUpdate.Result.UPDATED_TO_LATEST) {
            SandboxBootstrap.LOG.info("A new update has been installed. Please restart your server to apply changes");
            System.exit(5480);
        }
    }

    static Runnable closeCallback = () -> {};

    public static void updateClient() {
        @Nullable String headless = System.setProperty("java.awt.headless", "false");
        JFrame frame = new JFrame();
        closeCallback = () -> {
            frame.setVisible(false);
            frame.dispose();
            if (headless != null) {
                System.setProperty("java.awt.headless", headless);
            }
        };
        BufferedImage image;
        try {
            image = ImageIO.read(AutoUpdate.class.getResource("/banner.png"));
            frame.setIconImage(ImageIO.read(AutoUpdate.class.getResource("/icon.png")));
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
        JProgressBar progressBar = new JProgressBar(0, 4194304);
        progressBar.setPreferredSize(new Dimension(image.getWidth() - 300, 50));
        progressBar.setUI(new DownloadProgressUI());
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setForeground(Color.RED);
        //JLabel textLabel = new JLabel("", JLabel.CENTER);
        //textLabel.setForeground(Color.WHITE);
        label.setBounds(image.getWidth() / 2 - 50, image.getHeight() - 150, 100, 100);
        //textLabel.setBounds(0, image.getHeight() - 150, image.getWidth(), 100);

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
        //frame.add(textLabel);
        frame.add(panel);

        if (frame.isAlwaysOnTopSupported())
            frame.setAlwaysOnTop(true);
        frame.setSize(image.getWidth(), image.getHeight());
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);
        frame.setUndecorated(true);
        //textLabel.setVisible(false);
        frame.setVisible(true);

        //FIXME there should be a better way to do these
        //Consumer<String> info = s -> {
        //    SandboxBootstrap.LOG.info(s);
        //    textLabel.setText(s);
        //    label.setVisible(false);
        //    textLabel.setVisible(true);
        //};
//
        //BiConsumer<String, Exception> error = (s, e) -> {
        //    SandboxBootstrap.LOG.error(s, e);
        //    textLabel.setText("Error: " + s + " - " + e.getMessage());
        //    label.setVisible(false);
        //    textLabel.setVisible(true);
        //};

        ProgressCallback updateProgress = (bytesDownloaded, bytesTotal, stage) -> SwingUtilities.invokeLater(() -> {
            switch (stage) {
                case PREPARING:
                    progressBar.setVisible(true);
                    progressBar.setValue(0);
                    progressBar.setString("0%");
                    break;
                case DOWNLOADING:
                    //Using a high number for more precision
                    progressBar.setValue((int) (bytesDownloaded / (double) bytesTotal * 4194304.0));
                    progressBar.setString((int) (bytesDownloaded / (double) bytesTotal * 100.0) + "%");
                    break;
                case COPYING:
                    progressBar.setValue(4194304);
                    progressBar.setString("100% - cleaning up");
                    break;
                case FINISHED:
                    progressBar.setString("100% - done!");
                    progressBar.setVisible(false);
            }
        });

        if (SandboxUpdateChecker.check(updateProgress) == Result.UPDATED_TO_LATEST) {
            //textLabel.setText("A new update has been installed. Please restart your client to apply changes");
            JOptionPane.showMessageDialog(frame, "A new update has been installed. Please restart your client to apply changes");
            closeCallback.run();
            System.exit(5480);
        }
    }

    public static void closeClientWindow() {
        closeCallback.run();
    }

    public enum Result {
        ON_LATEST,
        UPDATED_TO_LATEST,
        UNABLE_TO_DOWNLOAD,
        UNABLE_TO_CHECK
    }

    private static class DownloadProgressUI extends BasicProgressBarUI {
        @Override
        protected void paintIndeterminate(Graphics graphics, JComponent component) {
            Rectangle r = getBox(new Rectangle());
            ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(progressBar.getForeground());
            graphics.fillOval(r.x, r.y, r.width, r.height);
        }
    }
}
