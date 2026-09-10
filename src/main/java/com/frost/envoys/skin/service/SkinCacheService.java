package com.frost.envoys.skin.service;

import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.skin.model.SkinMetaData;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class SkinCacheService {

    public static String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String calculateSHA256(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static void saveToCache(Path cacheDir, String fileKey, byte[] pngData, SkinMetaData meta) throws Exception {
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }

        String cleanKey = fileKey.toLowerCase().endsWith(".png") ? fileKey.substring(0, fileKey.length() - 4) : fileKey;

        Path pngPath = cacheDir.resolve(cleanKey + ".png");
        Path jsonPath = cacheDir.resolve(cleanKey + ".json");

        Files.write(pngPath, pngData);
        String json = EntityActionAdapter.GSON.toJson(meta);
        Files.writeString(jsonPath, json);
    }
}