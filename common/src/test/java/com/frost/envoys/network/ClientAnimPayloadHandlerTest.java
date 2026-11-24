package com.frost.envoys.network;

import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.network.payload.AnimRemovedPayload;
import com.frost.envoys.util.ClientPathManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAnimPayloadHandlerTest {

    @TempDir
    Path tempDir;

    private NetworkTestSupport.RecordingNetworkManager network;

    @BeforeEach
    void setUp() {
        EmoteIntegration.clearServerData();
        network = new NetworkTestSupport.RecordingNetworkManager();
        Network.init(network);
    }

    @AfterEach
    void tearDown() {
        Network.init(null);
        EmoteIntegration.clearServerData();
    }

    @Test
    void nullAnimListIsIgnored() {
        ClientAnimPayloadHandler.handleAnimList(
                new AnimListPayload(null), NetworkTestSupport.context(null));

        assertFalse(EmoteIntegration.serverIndexLoaded());
        assertTrue(network.toServer().isEmpty());
    }

    @Test
    void emptyAnimListMarksIndexLoaded() {
        ClientAnimPayloadHandler.handleAnimList(
                new AnimListPayload(List.of()), NetworkTestSupport.context(null));

        assertTrue(EmoteIntegration.serverIndexLoaded());
        assertTrue(network.toServer().isEmpty());
    }

    @Test
    void emptyAnimDataMarksAnimationMissing() {
        ClientAnimPayloadHandler.handleAnimData(
                new AnimDataPayload("walk", new byte[0]), NetworkTestSupport.context(null));

        EmoteIntegration.requestIfMissing("walk");

        assertTrue(network.toServer().isEmpty());
    }

    @Test
    void blankRemovedAnimationIsIgnored() throws Exception {
        Path animDir = Files.createDirectories(tempDir.resolve("anim"));
        Path unknown = Files.writeString(animDir.resolve("unknown.json"), "{}");

        try (MockedStatic<ClientPathManager> paths = Mockito.mockStatic(ClientPathManager.class)) {
            paths.when(ClientPathManager::getClientAnimDir).thenReturn(animDir);

            ClientAnimPayloadHandler.handleAnimRemoved(
                    new AnimRemovedPayload("   "), NetworkTestSupport.context(null));

            paths.verify(ClientPathManager::getClientAnimDir, Mockito.never());
        }

        assertTrue(Files.exists(unknown));
        assertTrue(network.toServer().isEmpty());
    }
}
