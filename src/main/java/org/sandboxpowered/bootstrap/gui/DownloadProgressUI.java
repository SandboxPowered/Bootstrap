package org.sandboxpowered.bootstrap.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;

class DownloadProgressUI extends BasicProgressBarUI {
    @Override
    protected void paintIndeterminate(Graphics graphics, JComponent component) {
        Rectangle r = getBox(new Rectangle());
        ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(progressBar.getForeground());
        graphics.fillOval(r.x, r.y, r.width, r.height);
    }
}
