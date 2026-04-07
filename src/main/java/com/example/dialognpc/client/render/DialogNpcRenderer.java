package com.example.dialognpc.client.render;

import com.example.dialognpc.entity.DialogNpcEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class DialogNpcRenderer extends MobEntityRenderer<DialogNpcEntity, PlayerEntityModel<DialogNpcEntity>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogNpcRenderer.class);
    private static final Identifier FALLBACK =
        Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    // Cache for dynamic textures (URL/base64)
    private static final Map<String, Identifier> DYNAMIC_TEXTURES = new HashMap<>();

    {
        // Static initializer to confirm renderer is loaded
        System.out.println("[DialogNpcRenderer] Renderer class loaded!");
    }

    public DialogNpcRenderer(EntityRendererFactory.Context ctx) {
        // false = slim=false (Steve model). Change to true for Alex/slim model.
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    private static int callCount = 0;

    @Override
    public Identifier getTexture(DialogNpcEntity entity) {
        callCount++;
        String textureType = entity.getTextureType();
        String customData = entity.getCustomTextureData();

        // Solo loggea cuando NO es vanilla o cada 100 llamadas para no spamear
        if (!"vanilla".equals(textureType) || callCount % 100 == 1) {
            System.out.println("[DialogNpcRenderer] [" + callCount + "] getTexture | type=" + textureType + ", customData=" + customData);
        }

        switch (textureType) {
            case "player":
                String playerName = entity.getCustomTextureData();
                System.out.println("[DialogNpcRenderer] [" + callCount + "] Loading player skin: '" + playerName + "'");
                if (playerName != null && !playerName.isEmpty()) {
                    Identifier result = loadPlayerSkin(playerName);
                    System.out.println("[DialogNpcRenderer] [" + callCount + "] Player skin result: " + result);
                    return result;
                }
                System.out.println("[DialogNpcRenderer] [" + callCount + "] Empty player name, returning FALLBACK");
                return FALLBACK;

            case "url":
                String url = entity.getCustomTextureData();
                System.out.println("[DialogNpcRenderer] [" + callCount + "] Loading URL texture: " + url);
                if (url != null && !url.isEmpty()) {
                    return loadTextureFromUrl(url);
                }
                return FALLBACK;

            case "base64":
                String base64Data = entity.getCustomTextureData();
                System.out.println("[DialogNpcRenderer] [" + callCount + "] Loading base64 texture");
                if (base64Data != null && !base64Data.isEmpty()) {
                    return loadTextureFromBase64(base64Data, entity.getUuidAsString());
                }
                return FALLBACK;

            case "vanilla":
            default:
                String npcTexture = entity.getNpcTexture();
                Identifier id = Identifier.tryParse(npcTexture);
                return id != null ? id : FALLBACK;
        }
    }

    private Identifier loadPlayerSkin(String playerName) {
        String cacheKey = "player_" + playerName.toLowerCase();
        Identifier cached = DYNAMIC_TEXTURES.get(cacheKey);
        if (cached != null) {
            System.out.println("[DialogNpcRenderer] Using CACHED skin for: " + playerName);
            return cached;
        }

        System.out.println("[DialogNpcRenderer] No cache for " + playerName + ", loading synchronously...");

        // Load skin synchronously (blocking) - waits until texture is registered
        boolean success = loadPlayerSkinSync(playerName, cacheKey);

        if (success) {
            // Wait until texture is actually registered
            int waitAttempts = 0;
            while (DYNAMIC_TEXTURES.get(cacheKey) == null && waitAttempts < 50) {
                try {
                    Thread.sleep(10);
                    waitAttempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            Identifier result = DYNAMIC_TEXTURES.get(cacheKey);
            if (result != null) {
                System.out.println("[DialogNpcRenderer] Skin loaded and registered: " + result);
                return result;
            }
        }

        System.out.println("[DialogNpcRenderer] Failed to load skin for " + playerName + ", using FALLBACK");
        return FALLBACK;
    }

    private boolean loadPlayerSkinSync(String playerName, String cacheKey) {
        try {
            System.out.println("[DialogNpcRenderer] Fetching skin from mc-heads.net for: " + playerName);

            // Use mc-heads.net API - simple and reliable
            String skinUrl = "https://mc-heads.net/skin/" + playerName;
            java.net.URI uri = java.net.URI.create(skinUrl);

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestProperty("User-Agent", "DialogNpcMod/1.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            System.out.println("[DialogNpcRenderer] HTTP Response: " + responseCode);

            if (responseCode != 200) {
                System.err.println("[DialogNpcRenderer] mc-heads.net returned HTTP " + responseCode + " for " + playerName);
                conn.disconnect();
                return false;
            }

            java.io.InputStream stream = conn.getInputStream();
            byte[] bytes = stream.readAllBytes();
            stream.close();
            conn.disconnect();

            System.out.println("[DialogNpcRenderer] Received " + bytes.length + " bytes");

            if (bytes.length == 0) {
                System.err.println("[DialogNpcRenderer] Empty response from mc-heads.net for " + playerName);
                return false;
            }

            NativeImage image = NativeImage.read(bytes);
            System.out.println("[DialogNpcRenderer] Image decoded: " + image.getWidth() + "x" + image.getHeight());

            Identifier textureId = Identifier.of("dialognpc", "player_" + playerName.toLowerCase());

            // Register texture immediately on current thread
            registerTexture(textureId, image);
            DYNAMIC_TEXTURES.put(cacheKey, textureId);

            System.out.println("[DialogNpcRenderer] Skin registered immediately: " + textureId);

            return true;

        } catch (Exception e) {
            System.err.println("[DialogNpcRenderer] Error loading skin for " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Identifier loadTextureFromUrl(String url) {
        return DYNAMIC_TEXTURES.computeIfAbsent(url, key -> {
            try {
                // Download and load texture from URL
                java.net.URI uri = java.net.URI.create(url);
                java.io.InputStream stream = uri.toURL().openStream();
                NativeImage image = NativeImage.read(stream);
                stream.close();

                Identifier textureId = Identifier.of("dialognpc", "url_" + hashUrl(url));
                registerTexture(textureId, image);
                LOGGER.info("Loaded NPC texture from URL: {}", url);
                return textureId;
            } catch (Exception e) {
                LOGGER.error("Failed to load NPC texture from URL: {}", url, e);
                return FALLBACK;
            }
        });
    }

    private Identifier loadTextureFromBase64(String base64Data, String uniqueId) {
        String cacheKey = "base64_" + hashUrl(base64Data);
        return DYNAMIC_TEXTURES.computeIfAbsent(cacheKey, key -> {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(imageBytes);
                NativeImage image = NativeImage.read(stream);
                stream.close();

                Identifier textureId = Identifier.of("dialognpc", "base64_" + uniqueId.substring(0, 8));
                registerTexture(textureId, image);
                LOGGER.info("Loaded NPC texture from base64 data");
                return textureId;
            } catch (Exception e) {
                LOGGER.error("Failed to load NPC texture from base64", e);
                return FALLBACK;
            }
        });
    }

    private void registerTexture(Identifier id, NativeImage image) {
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
    }

    private static String hashUrl(String str) {
        return String.valueOf(Math.abs(str.hashCode()));
    }
}
