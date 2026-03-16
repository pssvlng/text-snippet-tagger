package com.snippettray.tray;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class TrayIconFactory {
    private TrayIconFactory() {
    }

    public static BufferedImage createIconImage(int size) {
        int safeSize = Math.max(size, 16);
        BufferedImage image = new BufferedImage(safeSize, safeSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(36, 52, 71));
        g.fillRoundRect(1, 1, safeSize - 2, safeSize - 2, safeSize / 3, safeSize / 3);

        g.setColor(new Color(255, 255, 255, 230));
        g.setStroke(new BasicStroke(Math.max(2f, safeSize / 8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int margin = safeSize / 4;
        int midY = safeSize / 2;

        g.drawLine(margin, midY, safeSize / 2 - 1, margin);
        g.drawLine(margin, midY, safeSize / 2 - 1, safeSize - margin);
        g.drawLine(safeSize - margin, midY, safeSize / 2 + 1, margin);
        g.drawLine(safeSize - margin, midY, safeSize / 2 + 1, safeSize - margin);

        g.dispose();
        return image;
    }
}
