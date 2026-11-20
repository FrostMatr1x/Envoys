package com.frost.envoys.skin.service;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.model.SkinIndexEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class SkinLocalService {

    public static boolean isSafeSkinFileName(String path) {
        return path != null
                && !path.startsWith(".")
                && path.matches("[A-Za-z0-9._-]+\\.png");
    }

    public static SkinIndexData readIndex(File indexFile) {
        if (!indexFile.exists()) {
            return new SkinIndexData();
        }
        try {
            String content = Files.readString(indexFile.toPath());
            SkinIndexData data = EntityActionAdapter.GSON.fromJson(content, SkinIndexData.class);
            if (data == null) {
                return new SkinIndexData();
            }
            if (data.skins == null) {
                data.skins = new ArrayList<>();
            }
            data.skins.removeIf(e -> e == null || e.uuid == null || e.path == null);
            return data;
        } catch (Exception e) {
            EnvoysConstants.LOGGER.error("Failed to read skin index: {}", indexFile.getAbsolutePath(), e);
            return new SkinIndexData();
        }
    }

    public static synchronized void saveIndex(File indexFile, SkinIndexData data) {
        try {
            if (!indexFile.getParentFile().exists()) {
                indexFile.getParentFile().mkdirs();
            }
            String json = EntityActionAdapter.GSON.toJson(data);
            Files.writeString(indexFile.toPath(), json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            EnvoysConstants.LOGGER.error("Failed to save skin index: {}", indexFile.getAbsolutePath(), e);
        }
    }

    public static synchronized void addOrUpdateIndex(File indexFile, SkinIndexEntry entry) {
        SkinIndexData index = readIndex(indexFile);
        index.skins.removeIf(e -> e.uuid.equalsIgnoreCase(entry.uuid));
        index.skins.add(entry);
        saveIndex(indexFile, index);
    }
    
    public static Optional<Path> findSkinRecursively(Path rootDir, String identifier) {
        if (!Files.exists(rootDir)) return Optional.empty();

        final List<Path> matches = new ArrayList<>();
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.getFileName().toString().equalsIgnoreCase("cache")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    if (fileName.toLowerCase().endsWith(".png")) {
                        String nameWithoutExt = fileName.substring(0, fileName.length() - 4);
                        if (nameWithoutExt.equalsIgnoreCase(identifier) || fileName.equalsIgnoreCase(identifier)) {
                            matches.add(file);
                            return FileVisitResult.TERMINATE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            EnvoysConstants.LOGGER.error("Failed to scan skin directory {}", rootDir, e);
        }

        return matches.stream().findFirst();
    }
}