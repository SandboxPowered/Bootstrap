package org.sandboxpowered.bootstrap.gui;

import org.jetbrains.annotations.Nullable;
import org.sandboxpowered.bootstrap.AutoUpdate;
import org.sandboxpowered.bootstrap.SandboxBootstrap;
import org.sandboxpowered.bootstrap.util.download.DownloadHandler;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class GuiDownloadHandler implements DownloadHandler {

    private final JFrame window = new JFrame();
    private final JProgressBar progressBar = new JProgressBar(0, 4194304);
    @Nullable
    private String wasHeadless;

    @Override
    public void onClose() {
        window.setVisible(false);
        window.dispose();
        if (wasHeadless != null) {
            System.setProperty("java.awt.headless", wasHeadless);
        }
    }

    @Override
    public void onFinishedDownloading() {
        //textLabel.setText("An update has been installed. Please restart your client to apply changes");
        JOptionPane.showMessageDialog(window, "An update has been installed. Please restart your client to apply changes", "Sandbox", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onStartDownloading() {
        wasHeadless = System.setProperty("java.awt.headless", "false");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            SandboxBootstrap.LOG.error("Unable to set Swing Look-And-Feel", e);
        }
        BufferedImage image;
        try {
            image = ImageIO.read(AutoUpdate.class.getResource("/sboxbootstrap/banner.png"));
            window.setIconImage(ImageIO.read(AutoUpdate.class.getResource("/sboxbootstrap/icon.png")));
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
        window.add(label);
        //frame.add(textLabel);
        window.add(panel);

        window.setSize(image.getWidth(), image.getHeight());
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        window.setLocation(dim.width / 2 - window.getSize().width / 2, dim.height / 2 - window.getSize().height / 2);
        window.setUndecorated(true);
        //textLabel.setVisible(false);
        window.setVisible(true);

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
    }

    @Override
    public void accept(long bytesDownloaded, long bytesTotal, Stage stage) {
        AutoUpdate.HEADLESS_PROGRESS_CALLBACK.accept(bytesDownloaded, bytesTotal, stage);
        SwingUtilities.invokeLater(() -> {
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
                    //textLabel.setText("An update has been installed. Please restart your client to apply changes");
                    JOptionPane.showMessageDialog(window, "An update has been installed. Please restart your client to apply changes");
            }
        });
    }
}
