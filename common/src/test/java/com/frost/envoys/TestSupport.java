package com.frost.envoys;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.client.ClientLuaSaveTracker;
import com.frost.envoys.network.RequestRateLimiter;
import com.frost.envoys.quest.QuestIndex;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public final class TestSupport {

    private TestSupport() {
    }

    public static UUID uuid(int value) {
        return new UUID(0L, value);
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
