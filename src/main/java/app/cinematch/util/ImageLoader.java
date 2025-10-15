package app.cinematch.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageLoader {
    private static final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();

    public static ImageIcon loadPoster(String url, int maxW, int maxH) {
        if (url == null || url.isBlank()) return null;
        if (cache.containsKey(url)) return cache.get(url);
        try {
            BufferedImage img = ImageIO.read(new URL(url));
            if (img == null) return null;

            int w = img.getWidth(), h = img.getHeight();
            double scale = Math.min((double) maxW / w, (double) maxH / h);

            Image scaled = img;
            if (scale < 1.0) {
                scaled = img.getScaledInstance(
                        (int) (w * scale),
                        (int) (h * scale),
                        Image.SCALE_SMOOTH
                );
            }

            ImageIcon icon = new ImageIcon(scaled);
            cache.put(url, icon);
            return icon;
        } catch (IOException e) {
            System.err.println("[ImageLoader] Erreur de chargement : " + e.getMessage());
            return null;
        }
    }
}
