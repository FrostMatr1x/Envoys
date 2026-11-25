package com.frost.envoys.network;

import com.frost.envoys.TestSupport;
import com.frost.envoys.network.payload.RequestSkinPayload;
import com.frost.envoys.network.payload.SkinInfoPayload;
import com.frost.envoys.util.ClientPathManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ClientSkinPayloadHandlerTest {

    @TempDir
    Path tempDir;

    private NetworkTestSupport.RecordingNetworkManager network;

    @BeforeEach
    void setUp() {
        network = new NetworkTestSupport.RecordingNetworkManager();
        Network.init(network);
    }

    @AfterEach
    void tearDown() {
        Network.init(null);
    }

    @Test
    void missingCachedSkinRequestsItFromServer() {
        UUID id = TestSupport.uuid(1);
        try (MockedStatic<ClientPathManager> paths = Mockito.mockStatic(ClientPathManager.class)) {
            paths.when(ClientPathManager::getClientSkinCacheDir).thenReturn(tempDir.resolve("cache"));

            ClientSkinPayloadHandler.handleSkinInfo(
                    new SkinInfoPayload(id, "", "hash", "classic"), NetworkTestSupport.context(null));

            assertEquals(1, network.toServer().size());
            assertInstanceOf(RequestSkinPayload.class, network.toServer().get(0));
            RequestSkinPayload request = (RequestSkinPayload) network.toServer().get(0);
            assertEquals(id, request.npcUuid());
        }
    }
}
