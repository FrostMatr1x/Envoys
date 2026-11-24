package com.frost.envoys;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.client.ClientLuaSaveTracker;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.npc.merchant.TradeCounterStore;
import com.frost.envoys.network.RequestRateLimiter;
import com.frost.envoys.platform.EnvoysPlatform;
import com.frost.envoys.platform.TestPlatform;
import com.frost.envoys.quest.QuestIndex;
import com.frost.envoys.util.PathManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class TestSupport {

    private TestSupport() {
    }

    public static UUID uuid(int value) {
        return new UUID(0L, value);
    }

    public static void installEnvironment(Path tempDir) {
        try {
            Path gameDir = Files.createDirectories(tempDir.resolve("game"));
            Path configDir = Files.createDirectories(tempDir.resolve("config"));
            Path worldDir = Files.createDirectories(tempDir.resolve("world"));
            EnvoysPlatform.init(new TestPlatform(gameDir, configDir));
            setStaticField(PathManager.class, "serverWorldDir", worldDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void clearEnvironment() {
        PathManager.clearServer();
        EnvoysPlatform.init(null);
        NPCInteractManager.SCRIPTS.clear();
        QuestIndex.invalidate();
    }

    public static void resetNpcConfigMigration() {
        setStaticField(NPCConfigManager.class, "migratedDir", null);
    }

    public static void resetTradeCounterState() {
        try {
            Field data = TradeCounterStore.class.getDeclaredField("DATA");
            data.setAccessible(true);
            ((Map<?, ?>) data.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        setStaticField(TradeCounterStore.class, "migratedDir", null);
    }

    public static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void shutdownExecutor(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof ExecutorService executor) {
                executor.shutdownNow();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void resetQuestState() {
        NPCInteractManager.SCRIPTS.clear();
        QuestIndex.invalidate();
    }

    public static void resetRequestRateLimiter() {
        try {
            Field requests = RequestRateLimiter.class.getDeclaredField("REQUESTS");
            requests.setAccessible(true);
            ((Map<?, ?>) requests.get(null)).clear();

            Field lastCleanup = RequestRateLimiter.class.getDeclaredField("lastCleanup");
            lastCleanup.setAccessible(true);
            lastCleanup.setLong(null, System.currentTimeMillis());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void resetClientLuaSaveTracker() {
        try {
            Field pending = ClientLuaSaveTracker.class.getDeclaredField("PENDING");
            pending.setAccessible(true);
            ((Map<?, ?>) pending.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] png64(boolean transparentPixel54_20) {
        return png(64, 64, transparentPixel54_20 ? 0x00000000 : 0xFF000000);
    }

    public static byte[] png(int width, int height, int argbAt54_20) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (width > 54 && height > 20) {
                image.setRGB(54, 20, argbAt54_20);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
