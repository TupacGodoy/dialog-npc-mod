package com.example.dialognpc;

import com.example.dialognpc.client.render.DialogNpcRenderer;
import com.example.dialognpc.entity.ModEntities;
import com.example.dialognpc.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class DialogNpcModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DIALOG_NPC, DialogNpcRenderer::new);
        ModPackets.registerClientPackets();
    }
}
