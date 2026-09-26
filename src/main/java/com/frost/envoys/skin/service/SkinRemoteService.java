package com.frost.envoys.skin.service;

import com.frost.envoys.Envoys;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

public class SkinRemoteService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public record MojangSkinResult(String textureUrl, String model) {}

    private static void validateUriForSSRF(URI uri) throws Exception {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Invalid host in URL!");
        }

        InetAddress[] addresses = InetAddress.getAllByName(host);
        for (InetAddress addr : addresses) {
            if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || 
                addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || 
                addr.isMulticastAddress()) {
                throw new IllegalArgumentException("Access to local and private addresses is forbidden!");
            }
        }
    }

    private static HttpResponse<InputStream> sendWithSSRFAndRedirects(String urlString) throws Exception {
        String url = urlString.trim();
        int maxRedirects = 5;

        for (int i = 0; i < maxRedirects; i++) {
            if (!url.toLowerCase().startsWith("https://") && !url.toLowerCase().startsWith("http://")) {
                throw new IllegalArgumentException("Only HTTP/HTTPS URLs are allowed!");
            }

            URI uri = URI.create(url);
            validateUriForSSRF(uri);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/png,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();

            // Если получен редирект (301, 302, 303, 307, 308)
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                Optional<String> location = response.headers().firstValue("Location");
                if (location.isPresent()) {
                    String nextUrl = location.get();
                    if (nextUrl.startsWith("/")) {
                        nextUrl = uri.getScheme() + "://" + uri.getHost() + nextUrl;
                    }
                    url = nextUrl;
                    response.body().close();
                    continue;
                }
            }

            return response;
        }
        throw new RuntimeException("Maximum number of redirects exceeded (5)");
    }

    public static byte[] downloadSkinFromUrl(String urlString) throws Exception {
        String url = urlString.trim();

        if (url.toLowerCase().startsWith("http://textures.minecraft.net/")) {
            url = "https://" + url.substring(7);
        } else if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            url = "https://" + url;
        }

        HttpResponse<InputStream> response = sendWithSSRFAndRedirects(url);

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error: " + response.statusCode());
        }

        int maxSizeBytes = 2 * 1024 * 1024;
        byte[] buffer = new byte[8192];
        int totalRead = 0;

        try (InputStream in = response.body(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                totalRead += bytesRead;
                if (totalRead > maxSizeBytes) {
                    throw new IllegalArgumentException("Skin is too large! (max 2MB)");
                }
                out.write(buffer, 0, bytesRead);
            }

            byte[] data = out.toByteArray();

            if (data.length < 8 || (data[0] & 0xFF) != 0x89 || data[1] != 'P' || data[2] != 'N' || data[3] != 'G') {
                throw new IllegalArgumentException("File is not a valid PNG!");
            }

            return data;
        }
    }

    public static Optional<MojangSkinResult> fetchMojangSkin(String username) {
        try {
            String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
            HttpResponse<InputStream> resp1 = sendWithSSRFAndRedirects(uuidUrl);

            if (resp1.statusCode() != 200) return Optional.empty();

            String body1;
            try (InputStream in = resp1.body()) {
                body1 = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject obj1 = JsonParser.parseString(body1).getAsJsonObject();
            String uuid = obj1.get("id").getAsString();

            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
            HttpResponse<InputStream> resp2 = sendWithSSRFAndRedirects(profileUrl);

            if (resp2.statusCode() != 200) return Optional.empty();

            String body2;
            try (InputStream in = resp2.body()) {
                body2 = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject obj2 = JsonParser.parseString(body2).getAsJsonObject();
            JsonArray properties = obj2.getAsJsonArray("properties");

            for (var elem : properties) {
                JsonObject prop = elem.getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String val = prop.get("value").getAsString();
                    String decoded = new String(Base64.getDecoder().decode(val));
                    JsonObject texObj = JsonParser.parseString(decoded).getAsJsonObject()
                            .getAsJsonObject("textures")
                            .getAsJsonObject("SKIN");

                    String textureUrl = texObj.get("url").getAsString();
                    String model = "classic";
                    if (texObj.has("metadata")) {
                        JsonObject meta = texObj.getAsJsonObject("metadata");
                        if (meta.has("model")) {
                            model = meta.get("model").getAsString();
                        }
                    }
                    return Optional.of(new MojangSkinResult(textureUrl, model));
                }
            }
        } catch (Exception e) {
            Envoys.LOGGER.warn("Failed to load Mojang skin for {}: {}", username, e.getMessage());
        }
        return Optional.empty();
    }
}