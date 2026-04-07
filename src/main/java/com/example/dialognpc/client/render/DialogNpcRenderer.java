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

@Environment(EnvType.CLIENT)
public class DialogNpcRenderer extends MobEntityRenderer<DialogNpcEntity, PlayerEntityModel<DialogNpcEntity>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogNpcRenderer.class);
    private static final Identifier FALLBACK =
        Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    // Cache for dynamic textures (URL/base64)
    private static final Map<String, Identifier> DYNAMIC_TEXTURES = new HashMap<>();

    public DialogNpcRenderer(EntityRendererFactory.Context ctx) {
        // false = slim=false (Steve model). Change to true for Alex/slim model.
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public Identifier getTexture(DialogNpcEntity entity) {
        String textureType = entity.getTextureType();

        switch (textureType) {
            case "player":
                // Player skin by username - fetch from Mojang servers
                String playerName = entity.getCustomTextureData();
                if (playerName != null && !playerName.isEmpty()) {
                    return loadPlayerSkin(playerName);
                }
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
        return DYNAMIC_TEXTURES.computeIfAbsent(cacheKey, key -> {
            try {
                // Use Crafatar API for direct skin access (simpler, no JSON parsing)
                // Crafatar: https://crafatar.com/skins/{uuid} or /skins/{name}
                String skinUrl = "https://crafatar.com/skins/" + playerName;
                java.net.URI uri = java.net.URI.create(skinUrl);

                // Set user-agent to avoid being blocked
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestProperty("User-Agent", "DialogNpcMod/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                java.io.InputStream stream = conn.getInputStream();
                NativeImage image = NativeImage.read(stream);
                stream.close();
                conn.disconnect();

                Identifier textureId = Identifier.of("dialognpc", "player_" + playerName.toLowerCase());
                registerTexture(textureId, image);
                LOGGER.info("Loaded player skin for: {} from Crafatar", playerName);
                return textureId;
            } catch (Exception e) {
                LOGGER.error("Failed to load player skin for: {} - {}", playerName, e.getMessage());
                return FALLBACK;
            }
        });
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
