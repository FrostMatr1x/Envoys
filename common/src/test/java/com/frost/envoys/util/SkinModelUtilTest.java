package com.frost.envoys.util;

import com.frost.envoys.TestSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinModelUtilTest {

    @Test
    void transparentPixelMeansSlim() {
        assertEquals("slim", SkinModelUtil.detectModel(TestSupport.png64(true)));
    }

    @Test
    void opaquePixelMeansDefault() {
        assertEquals("default", SkinModelUtil.detectModel(TestSupport.png64(false)));
    }

    @Test
    void invalidOrSmallImagesFallBackToDefault() {
        assertEquals("default", SkinModelUtil.detectModel(null));
        assertEquals("default", SkinModelUtil.detectModel(new byte[0]));
        assertEquals("default", SkinModelUtil.detectModel("not a png".getBytes(StandardCharsets.UTF_8)));
        assertEquals("default", SkinModelUtil.detectModel(TestSupport.png(63, 63, 0)));
    }
}
