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

    @Override
    public Identifier getTexture(DialogNpcEntity entity) {
        String textureType = entity.getTextureType();
        System.out.println("[DialogNpcRenderer] getTexture called! textureType=" + textureType + ", customData=" + entity.getCustomTextureData());

        switch (textureType) {
            case "player":
                // Player skin by username - fetch from Mojang servers
                String playerName = entity.getCustomTextureData();
                System.out.println("[DialogNpcRenderer] Player skin requested for: " + playerName);
                if (playerName != null && !playerName.isEmpty()) {
                    Identifier result = loadPlayerSkin(playerName);
                    System.out.println("[DialogNpcRenderer] Returning texture: " + result);
                    return result;
                }
                System.out.println("[DialogNpcRenderer] Empty player name, returning FALLBACK");
                return FALLBACK;

            case "url":
                // URL texture - load dynamically
                String url = entity.getCustomTextureData();
                if (url != null && !url.isEmpty()) {
                    return loadTextureFromUrl(url);
                }
                return FALLBACK;

            case "base64":
                // Base64 encoded texture
                String base64Data = entity.getCustomTextureData();
                if (base64Data != null && !base64Data.isEmpty()) {
                    return loadTextureFromBase64(base64Data, entity.getUuidAsString());
                }
                return FALLBACK;

            case "vanilla":
            default:
                // Vanilla texture path
                String npcTexture = entity.getNpcTexture();
                Identifier id = Identifier.tryParse(npcTexture);
                return id != null ? id : FALLBACK;
        }
    }

    private Identifier loadPlayerSkin(String playerName) {
        String cacheKey = "player_" + playerName.toLowerCase();
        Identifier cached = DYNAMIC_TEXTURES.get(cacheKey);
        if (cached != null) {
            LOGGER.info("Using cached player skin for: {}", playerName);
            return cached;
        }

        // Try to load skin synchronously but on an IO thread
        // This is needed because getTexture() is called during rendering
        loadPlayerSkinSync(playerName, cacheKey);

        // Return the newly loaded texture (or fallback if failed)
        cached = DYNAMIC_TEXTURES.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LOGGER.warn("Using fallback texture for player: {}", playerName);
        return FALLBACK;
    }

    private void loadPlayerSkinSync(String playerName, String cacheKey) {
        // Run on IO thread pool to avoid blocking main thread
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Loading player skin for: {} from Crafatar", playerName);

                // Use Crafatar API for direct skin access
                String skinUrl = "https://crafatar.com/skins/" + playerName;
                java.net.URI uri = java.net.URI.create(skinUrl);

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestProperty("User-Agent", "DialogNpcMod/1.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);

                int responseCode = conn.getResponseCode();
                LOGGER.info("Crafatar HTTP response for {}: {}", playerName, responseCode);

                if (responseCode != 200) {
                    LOGGER.error("Crafatar returned HTTP {} for {}", responseCode, playerName);
                    return;
                }

                java.io.InputStream stream = conn.getInputStream();
                byte[] bytes = stream.readAllBytes();
                stream.close();
                conn.disconnect();

                if (bytes.length == 0) {
                    LOGGER.error("Empty response from Crafatar for {}", playerName);
                    return;
                }

                NativeImage image = NativeImage.read(bytes);
                LOGGER.info("Player skin loaded for {}: {}x{}, {} bytes", playerName, image.getWidth(), image.getHeight(), bytes.length);

                Identifier textureId = Identifier.of("dialognpc", "player_" + playerName.toLowerCase());

                // Register texture on main thread
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    try {
                        registerTexture(textureId, image);
                        DYNAMIC_TEXTURES.put(cacheKey, textureId);
                        LOGGER.info("Player skin registered for: {} -> {}", playerName, textureId);

                        // Mark renderer for update
                        mc.worldRenderer.reload();
                    } catch (Exception e) {
                        LOGGER.error("Failed to register texture for {}: {}", playerName, e.getMessage(), e);
                    }
                });

                // Wait a bit for the main thread to process
                Thread.sleep(50);

            } catch (Exception e) {
                LOGGER.error("Failed to load player skin for: {} - {}", playerName, e.getMessage(), e);
            }
        });

        // Wait briefly for async load to complete (max 500ms)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
