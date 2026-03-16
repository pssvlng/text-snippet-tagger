package com.snippettray.ui;

import com.snippettray.tray.TrayIconFactory;

import javax.swing.ImageIcon;
import java.awt.Image;

public final class AppAssets {
    private static final Image APP_IMAGE = TrayIconFactory.createIconImage(64);
    private static final ImageIcon APP_IMAGE_ICON = new ImageIcon(APP_IMAGE);

    private AppAssets() {
    }

    public static Image appImage() {
        return APP_IMAGE;
    }

    public static ImageIcon appImageIcon() {
        return APP_IMAGE_ICON;
    }
}
