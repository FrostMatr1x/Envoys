package com.frost.envoys.skin.service;

import com.frost.envoys.TestSupport;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinRemoteServiceTest {

    private static final int MAX_SKIN_BYTES = 2 * 1024 * 1024;

    @Test
    void normalizeDownloadUrlUpgradesTextureHost() {
        assertEquals("https://textures.minecraft.net/skin.png",
                SkinRemoteService.normalizeDownloadUrl("http://textures.minecraft.net/skin.png"));
        assertEquals("https://textures.minecraft.net/skin.png",
                SkinRemoteService.normalizeDownloadUrl("  http://textures.minecraft.net/skin.png  "));
        assertEquals("https://TEXTURES.MINECRAFT.NET/skin.png",
                SkinRemoteService.normalizeDownloadUrl("HTTP://TEXTURES.MINECRAFT.NET/skin.png"));
    }

    @Test
    void normalizeDownloadUrlAddsHttpsWhenSchemeMissing() {
        assertEquals("https://example.com/skin.png",
                SkinRemoteService.normalizeDownloadUrl("example.com/skin.png"));
    }

    @Test
    void normalizeDownloadUrlKeepsHttps() {
        assertEquals("https://example.com/skin.png",
                SkinRemoteService.normalizeDownloadUrl(" https://example.com/skin.png "));
    }

    @Test
    void ssrfRejectsLoopback() {
        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.validateUriForSSRF(URI.create("https://127.0.0.1/skin")));
        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.validateUriForSSRF(URI.create("https://localhost/skin")));
    }

    @Test
    void ssrfRejectsAnyLocalSiteLocalLinkLocalAndMulticast() {
        for (String host : List.of("0.0.0.0", "192.168.1.1", "169.254.1.1", "224.0.0.1")) {
            assertThrows(IllegalArgumentException.class,
                    () -> SkinRemoteService.validateUriForSSRF(URI.create("https://" + host + "/skin")),
                    host);
        }
    }

    @Test
    void ssrfAllowsPublicAddressLiteral() {
        assertDoesNotThrow(() -> SkinRemoteService.validateUriForSSRF(URI.create("https://8.8.8.8/skin")));
    }

    @Test
    void ssrfRejectsMissingHost() {
        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.validateUriForSSRF(URI.create("mailto:test@example.com")));
    }

    @Test
    void loopbackRequestIsRejectedBeforeServerIsContacted() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/skin.png", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/skin.png";
            assertThrows(IllegalArgumentException.class, () -> SkinRemoteService.downloadSkinFromUrl(url));
            assertEquals(0, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void followsRelativeRedirect() throws Exception {
        StubTransport transport = new StubTransport()
                .redirect("/next")
                .ok(pngBytes(8));
        StubSleeper sleeper = new StubSleeper();

        byte[] data = SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", sleeper);

        assertArrayEquals(pngBytes(8), data);
        assertEquals(2, transport.requests.size());
        assertEquals("https://8.8.8.8/next", transport.requests.get(1).uri().toString());
    }

    @Test
    void followsAbsoluteRedirect() throws Exception {
        StubTransport transport = new StubTransport()
                .redirect("https://8.8.8.8/other")
                .ok(pngBytes(8));

        SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", new StubSleeper());

        assertEquals("https://8.8.8.8/other", transport.requests.get(1).uri().toString());
    }

    @Test
    void redirectWithoutLocationFailsWithStatus() {
        StubTransport transport = new StubTransport().status(302, null, new byte[0]);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", new StubSleeper()));

        assertEquals("HTTP error: 302", error.getMessage());
    }

    @Test
    void tooManyRedirectsFail() {
        StubTransport transport = new StubTransport();
        for (int i = 0; i < 5; i++) {
            transport.redirect("/next");
        }

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", new StubSleeper()));

        assertEquals("Maximum number of redirects exceeded (5)", error.getMessage());
        assertEquals(5, transport.requests.size());
    }

    @Test
    void redirectToNonHttpSchemeIsRejected() {
        StubTransport transport = new StubTransport().redirect("ftp://8.8.8.8/skin");

        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", new StubSleeper()));
    }

    @Test
    void retriesRateLimitThenSucceeds() throws Exception {
        StubTransport transport = new StubTransport()
                .status(429, null, new byte[0])
                .ok(pngBytes(8));
        StubSleeper sleeper = new StubSleeper();

        assertArrayEquals(pngBytes(8),
                SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", sleeper));
        assertEquals(List.of(400L), sleeper.sleeps);
        assertEquals(2, transport.requests.size());
    }

    @Test
    void retriesServerErrorsThenSucceeds() throws Exception {
        StubTransport transport = new StubTransport()
                .status(503, null, new byte[0])
                .status(500, null, new byte[0])
                .ok(pngBytes(8));
        StubSleeper sleeper = new StubSleeper();

        assertArrayEquals(pngBytes(8),
                SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", sleeper));
        assertEquals(List.of(400L, 800L), sleeper.sleeps);
    }

    @Test
    void returnsLastResponseAfterRepeatedServerErrors() {
        StubTransport transport = new StubTransport()
                .status(500, null, new byte[0])
                .status(500, null, new byte[0])
                .status(500, null, new byte[0]);
        StubSleeper sleeper = new StubSleeper();

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", sleeper));

        assertEquals("HTTP error: 500", error.getMessage());
        assertEquals(List.of(400L, 800L), sleeper.sleeps);
        assertEquals(3, transport.requests.size());
    }

    @Test
    void non200ResponseThrows() {
        StubTransport transport = new StubTransport().status(404, null, new byte[0]);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", new StubSleeper()));

        assertEquals("HTTP error: 404", error.getMessage());
    }

    @Test
    void oversizedBodyIsRejected() {
        StubTransport transport = new StubTransport().ok(pngBytes(MAX_SKIN_BYTES + 1));

        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", new StubSleeper()));
    }

    @Test
    void nonPngBodyIsRejected() {
        StubTransport shortBody = new StubTransport().ok(new byte[]{1, 2, 3, 4});
        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(shortBody, "https://8.8.8.8/a", new StubSleeper()));

        StubTransport wrongSignature = new StubTransport().ok(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.downloadSkinFromUrl(wrongSignature, "https://8.8.8.8/a", new StubSleeper()));
    }

    @Test
    void validPngIsReturned() throws Exception {
        byte[] png = TestSupport.png64(false);
        StubTransport transport = new StubTransport().ok(png);

        assertArrayEquals(png,
                SkinRemoteService.downloadSkinFromUrl(transport, "https://8.8.8.8/a", new StubSleeper()));
    }

    @Test
    void readPngLimitedAcceptsExactlyMaxAndRejectsOneMore() throws Exception {
        assertArrayEquals(pngBytes(1024),
                SkinRemoteService.readPngLimited(new ByteArrayInputStream(pngBytes(1024)), 1024));

        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.readPngLimited(new ByteArrayInputStream(pngBytes(1025)), 1024));
    }

    @Test
    void readPngLimitedValidatesSignatureAndLength() {
        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.readPngLimited(new ByteArrayInputStream(pngBytes(7)), 1024));
        assertThrows(IllegalArgumentException.class,
                () -> SkinRemoteService.readPngLimited(new ByteArrayInputStream(new byte[8]), 1024));
    }

    @Test
    void fetchMojangSkinReturnsClassicSkin() {
        StubTransport transport = new StubTransport()
                .ok("{\"id\":\"abc\"}".getBytes(StandardCharsets.UTF_8))
                .ok(profileJson("https://textures.minecraft.net/texture/1", null).getBytes(StandardCharsets.UTF_8));

        Optional<SkinRemoteService.MojangSkinResult> result = SkinRemoteService.fetchMojangSkin(
                transport, "Notch", "https://8.8.8.8/uuid/", "https://8.8.8.8/profile/", new StubSleeper());

        assertTrue(result.isPresent());
        assertEquals("https://textures.minecraft.net/texture/1", result.get().textureUrl());
        assertEquals("classic", result.get().model());
        assertEquals("https://8.8.8.8/uuid/Notch", transport.requests.get(0).uri().toString());
        assertEquals("https://8.8.8.8/profile/abc", transport.requests.get(1).uri().toString());
    }

    @Test
    void fetchMojangSkinReadsSlimModel() {
        StubTransport transport = new StubTransport()
                .ok("{\"id\":\"abc\"}".getBytes(StandardCharsets.UTF_8))
                .ok(profileJson("https://textures.minecraft.net/texture/1", "slim")
                        .getBytes(StandardCharsets.UTF_8));

        Optional<SkinRemoteService.MojangSkinResult> result = SkinRemoteService.fetchMojangSkin(
                transport, "Notch", "https://8.8.8.8/uuid/", "https://8.8.8.8/profile/", new StubSleeper());

        assertTrue(result.isPresent());
        assertEquals("slim", result.get().model());
    }

    @Test
    void fetchMojangSkinReturnsEmptyOnNon200Steps() {
        StubTransport firstStep = new StubTransport().status(404, null, new byte[0]);
        assertTrue(SkinRemoteService.fetchMojangSkin(firstStep, "Notch",
                "https://8.8.8.8/uuid/", "https://8.8.8.8/profile/", new StubSleeper()).isEmpty());

        StubTransport secondStep = new StubTransport()
                .ok("{\"id\":\"abc\"}".getBytes(StandardCharsets.UTF_8))
                .status(404, null, new byte[0]);
        assertTrue(SkinRemoteService.fetchMojangSkin(secondStep, "Notch",
                "https://8.8.8.8/uuid/", "https://8.8.8.8/profile/", new StubSleeper()).isEmpty());
        assertEquals(2, secondStep.requests.size());
    }

    @Test
    void fetchMojangSkinReturnsEmptyOnMalformedUuidResponse() {
        StubTransport transport = new StubTransport()
                .ok("{not json".getBytes(StandardCharsets.UTF_8));

        assertTrue(SkinRemoteService.fetchMojangSkin(transport, "Notch",
                "https://8.8.8.8/uuid/", "https://8.8.8.8/profile/", new StubSleeper()).isEmpty());
    }

    @Test
    void parseMojangSkinProfileHandlesValidAndBrokenPayloads() {
        assertTrue(SkinRemoteService.parseMojangSkinProfile(
                profileJson("https://textures.minecraft.net/texture/1", null)).isPresent());
        assertEquals("slim", SkinRemoteService.parseMojangSkinProfile(
                profileJson("https://textures.minecraft.net/texture/1", "slim")).orElseThrow().model());

        assertTrue(SkinRemoteService.parseMojangSkinProfile("{\"properties\":[]}").isEmpty());
        assertTrue(SkinRemoteService.parseMojangSkinProfile("{}").isEmpty());
        assertTrue(SkinRemoteService.parseMojangSkinProfile("{").isEmpty());
        assertTrue(SkinRemoteService.parseMojangSkinProfile(
                "{\"properties\":[{\"name\":\"other\",\"value\":\"x\"}]}").isEmpty());
        assertTrue(SkinRemoteService.parseMojangSkinProfile(
                "{\"properties\":[{\"name\":\"textures\",\"value\":\"!!!\"}]}").isEmpty());
        assertTrue(SkinRemoteService.parseMojangSkinProfile(
                "{\"properties\":[{\"name\":\"textures\",\"value\":\""
                        + base64("{\"textures\":{}}") + "\"}]}").isEmpty());
        assertTrue(SkinRemoteService.parseMojangSkinProfile(
                "{\"properties\":[{\"name\":\"textures\",\"value\":\""
                        + base64("{\"textures\":{\"SKIN\":{}}}") + "\"}]}").isEmpty());
    }

    private static byte[] pngBytes(int total) {
        if (total < 8) {
            return new byte[total];
        }
        byte[] data = new byte[total];
        data[0] = (byte) 0x89;
        data[1] = 'P';
        data[2] = 'N';
        data[3] = 'G';
        data[4] = 0x0D;
        data[5] = 0x0A;
        data[6] = 0x1A;
        data[7] = 0x0A;
        return data;
    }

    private static String profileJson(String textureUrl, String model) {
        String metadata = model == null ? "" : ",\"metadata\":{\"model\":\"" + model + "\"}";
        String decoded = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"" + metadata + "}}}";
        return "{\"properties\":[{\"name\":\"textures\",\"value\":\"" + base64(decoded) + "\"}]}";
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class StubSleeper implements SkinRemoteService.Sleeper {
        private final List<Long> sleeps = new ArrayList<>();

        @Override
        public void sleep(long millis) {
            sleeps.add(millis);
        }
    }

    private static final class StubTransport implements SkinRemoteService.HttpTransport {
        private final Deque<HttpResponse<InputStream>> responses = new ArrayDeque<>();
        private final List<HttpRequest> requests = new ArrayList<>();

        StubTransport status(int status, Map<String, List<String>> headers, byte[] body) {
            responses.add(new StubResponse(status, headers, body));
            return this;
        }

        StubTransport redirect(String location) {
            return status(302, Map.of("Location", List.of(location)), new byte[0]);
        }

        StubTransport ok(byte[] body) {
            return status(200, null, body);
        }

        @Override
        public HttpResponse<InputStream> send(HttpRequest request) {
            requests.add(request);
            return responses.removeFirst();
        }
    }

    private static final class StubResponse implements HttpResponse<InputStream> {
        private final int status;
        private final HttpHeaders headers;
        private final InputStream body;

        StubResponse(int status, Map<String, List<String>> headers, byte[] body) {
            this.status = status;
            this.headers = HttpHeaders.of(headers == null ? Map.of() : headers, (a, b) -> true);
            this.body = new ByteArrayInputStream(body == null ? new byte[0] : body);
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public InputStream body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("https://8.8.8.8/");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
