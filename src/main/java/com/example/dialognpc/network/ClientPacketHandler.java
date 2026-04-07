package com.example.dialognpc.network;

import com.example.dialognpc.screen.DialogScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class ClientPacketHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OpenDialogPayload.ID, (payload, context) ->
            context.client().execute(() ->
                context.client().setScreen(
                    new DialogScreen(
                        payload.npcUuid(),
                        payload.title(),
                        payload.text(),
                        payload.texture(),
                        payload.textureType(),
                        payload.customTextureData(),
                        payload.npcName(),
                        payload.labels(),
                        payload.sounds(),
                        payload.particles(),
                        payload.particleCounts(),
                        payload.backgroundColor(),
                        payload.titleColor(),
                        payload.buttonWidth(),
                        payload.borderColor(),
                        payload.titleTextColor(),
                        payload.optionsHeight(),
                        payload.boxWidth(),
                        payload.boxHeight(),
                        payload.titleHeight(),
                        payload.boxPadding(),
                        payload.portraitSize()
                    )
                )
            )
        );
    }
}
