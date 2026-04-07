package com.example.dialognpc.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading textures from various sources.
 * Handles caching, player skins, URLs, and base64 data.
 */
public class TextureLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureLoader.class);
    private static final Identifier FALLBACK = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    // Shared texture cache
    private static final Map<String, Identifier> TEXTURE_CACHE = new HashMap<>();

    private TextureLoader() {
        // Utility class - prevent instantiation
    }

    /**
     * Load a texture based on type.
     * @param textureType One of: "player", "url", "base64", "vanilla"
     * @param customData Player name, URL, or base64 data (depending on type)
     * @param fallbackTexture Default texture if loading fails
     * @return Identifier for the loaded texture
     */
    public static Identifier loadTexture(String textureType, String customData, Identifier fallbackTexture) {
        if (fallbackTexture == null) {
            fallbackTexture = FALLBACK;
        }

        return switch (textureType != null ? textureType : "vanilla") {
            case "player" -> loadPlayerSkin(customData, fallbackTexture);
            case "url" -> loadTextureFromUrl(customData, fallbackTexture);
            case "base64" -> loadTextureFromBase64(customData, fallbackTexture);
            default -> loadVanillaTexture(customData, fallbackTexture);
        };
    }

    /**
     * Load texture with default fallback (Steve).
     */
    public static Identifier loadTexture(String textureType, String customData) {
        return loadTexture(textureType, customData, FALLBACK);
    }

    private static Identifier loadPlayerSkin(String playerName, Identifier fallback) {
        if (playerName == null || playerName.isEmpty()) {
            return fallback;
        }

        String cacheKey = "player_" + playerName.toLowerCase();
        Identifier cached = TEXTURE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String skinUrl = "https://mc-heads.net/skin/" + playerName;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(skinUrl))
                .header("User-Agent", "DialogNpcMod/1.0")
                .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body().length > 0) {
                NativeImage image = NativeImage.read(response.body());
                Identifier textureId = Identifier.of("dialognpc", "player_" + playerName.toLowerCase());
                registerTexture(textureId, image);
                TEXTURE_CACHE.put(cacheKey, textureId);
                return textureId;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load player skin for {}: {}", playerName, e.getMessage());
        }

        return fallback;
    }

    private static Identifier loadTextureFromUrl(String url, Identifier fallback) {
        if (url == null || url.isEmpty()) {
            return fallback;
        }

        Identifier cached = TEXTURE_CACHE.get(url);
        if (cached != null) {
            return cached;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "DialogNpcMod/1.0")
                .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body().length > 0) {
                NativeImage image = NativeImage.read(response.body());
                Identifier textureId = Identifier.of("dialognpc", "url_" + Math.abs(url.hashCode()));
                registerTexture(textureId, image);
                TEXTURE_CACHE.put(url, textureId);
                return textureId;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load texture from URL {}: {}", url, e.getMessage());
        }

        return fallback;
    }

    private static Identifier loadTextureFromBase64(String base64Data, Identifier fallback) {
        if (base64Data == null || base64Data.isEmpty()) {
            return fallback;
        }

        String cacheKey = "base64_" + Math.abs(base64Data.hashCode());
        Identifier cached = TEXTURE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String data = base64Data;
            if (data.startsWith("data:image")) {
                int commaIdx = data.indexOf(",");
                if (commaIdx > 0) data = data.substring(commaIdx + 1);
            }

            byte[] imageBytes = Base64.getDecoder().decode(data);
            NativeImage image = NativeImage.read(new ByteArrayInputStream(imageBytes));
            Identifier textureId = Identifier.of("dialognpc", "base64_" + Math.abs(base64Data.hashCode()));
            registerTexture(textureId, image);
            TEXTURE_CACHE.put(cacheKey, textureId);
            return textureId;
        } catch (Exception e) {
            LOGGER.warn("Failed to load texture from base64: {}", e.getMessage());
        }

        return fallback;
    }

    private static Identifier loadVanillaTexture(String texturePath, Identifier fallback) {
        if (texturePath == null || texturePath.isEmpty()) {
            return fallback;
        }

        Identifier id = Identifier.tryParse(texturePath);
        return id != null ? id : fallback;
    }

    private static void registerTexture(Identifier id, NativeImage image) {
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
    }

    /**
     * Clear the texture cache (useful for development/debugging).
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
    }
}
