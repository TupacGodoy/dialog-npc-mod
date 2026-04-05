package com.example.dialognpc.client.render;

import com.example.dialognpc.entity.DialogNpcEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DialogNpcRenderer extends MobEntityRenderer<DialogNpcEntity, PlayerEntityModel<DialogNpcEntity>> {

    private static final Identifier FALLBACK =
        Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    public DialogNpcRenderer(EntityRendererFactory.Context ctx) {
        // false = slim=false (Steve model). Change to true for Alex/slim model.
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public Identifier getTexture(DialogNpcEntity entity) {
        Identifier id = Identifier.tryParse(entity.getNpcTexture());
        return id != null ? id : FALLBACK;
    }
}
