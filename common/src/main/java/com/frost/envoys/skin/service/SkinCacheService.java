package com.frost.envoys.skin.service;

import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.skin.model.SkinMetaData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
        Path pngTmp = cacheDir.resolve(cleanKey + ".png.tmp");
        Path jsonTmp = cacheDir.resolve(cleanKey + ".json.tmp");

        try {
            Files.write(pngTmp, pngData,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            moveReplacing(pngTmp, pngPath);

            String json = EntityActionAdapter.GSON.toJson(meta);
            Files.writeString(jsonTmp, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            moveReplacing(jsonTmp, jsonPath);
        } catch (Exception e) {
            deleteQuietly(pngTmp);
            deleteQuietly(jsonTmp);
            throw e;
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}