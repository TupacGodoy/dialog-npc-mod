package com.example.dialognpc.client.render;

import com.example.dialognpc.entity.DialogNpcEntity;
import com.example.dialognpc.util.TextureLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class DialogNpcRenderer extends MobEntityRenderer<DialogNpcEntity, PlayerEntityModel<DialogNpcEntity>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogNpcRenderer.class);
    private static final Identifier FALLBACK =
        Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    public DialogNpcRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public Identifier getTexture(DialogNpcEntity entity) {
        String textureType = entity.getTextureType();
        String customData = entity.getCustomTextureData();
        String npcTexture = entity.getNpcTexture();

        // Use shared TextureLoader utility for all texture loading
        return TextureLoader.loadTexture(textureType, customData, Identifier.tryParse(npcTexture));
    }
}
