package com.frost.envoys.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

public class SkinModelUtil {

    public static String detectModel(byte[] pngData) {
        if (pngData == null || pngData.length == 0) {
            return "default";
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pngData)) {
            BufferedImage image = ImageIO.read(bais);
            if (image != null && image.getWidth() >= 64 && image.getHeight() >= 64) {
                int alpha = (image.getRGB(54, 20) >> 24) & 0xff;
                return (alpha == 0) ? "slim" : "default";
            }
        } catch (Exception ignored) {
        }
        return "default";
    }
}
