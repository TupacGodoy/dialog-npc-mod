package com.example.dialognpc.network;

import com.example.dialognpc.DialogNpcMod;
import com.example.dialognpc.entity.DialogNpcEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Networking for 1.21.1 using the CustomPayload API.
 *
 * SECURITY: Clients send only (npcUUID, optionIndex).
 * Commands are resolved server-side from NBT — clients never see command text.
 */
public class ModPackets {

    // ── S2C: server opens dialog on client ───────────────────────────────

    public record OpenDialogPayload(
        UUID   npcUuid,
        String title,
        String text,
        String texture,
        List<String> labels
    ) implements CustomPayload {

        public static final CustomPayload.Id<OpenDialogPayload> ID =
            new CustomPayload.Id<>(Identifier.of(DialogNpcMod.MOD_ID, "open_dialog"));

        public static final PacketCodec<PacketByteBuf, OpenDialogPayload> CODEC =
            new PacketCodec<>() {
                @Override
                public OpenDialogPayload decode(PacketByteBuf buf) {
                    UUID   uuid    = buf.readUuid();
                    String title   = buf.readString();
                    String text    = buf.readString();
                    String texture = buf.readString();
                    int count = buf.readVarInt();
                    List<String> labels = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) labels.add(buf.readString());
                    return new OpenDialogPayload(uuid, title, text, texture, labels);
                }

                @Override
                public void encode(PacketByteBuf buf, OpenDialogPayload p) {
                    buf.writeUuid(p.npcUuid());
                    buf.writeString(p.title());
                    buf.writeString(p.text());
                    buf.writeString(p.texture());
                    buf.writeVarInt(p.labels().size());
                    p.labels().forEach(buf::writeString);
                }
            };

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── C2S: client clicks an option ─────────────────────────────────────

    public record RunOptionPayload(UUID npcUuid, int optionIndex) implements CustomPayload {

        public static final CustomPayload.Id<RunOptionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(DialogNpcMod.MOD_ID, "run_option"));

        public static final PacketCodec<PacketByteBuf, RunOptionPayload> CODEC =
            new PacketCodec<>() {
                @Override
                public RunOptionPayload decode(PacketByteBuf buf) {
                    return new RunOptionPayload(buf.readUuid(), buf.readVarInt());
                }

                @Override
                public void encode(PacketByteBuf buf, RunOptionPayload p) {
                    buf.writeUuid(p.npcUuid());
                    buf.writeVarInt(p.optionIndex());
                }
            };

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── Registration ─────────────────────────────────────────────────────

    public static void registerServerPackets() {
        // Register payload types in the global registry
        PayloadTypeRegistry.playS2C().register(OpenDialogPayload.ID, OpenDialogPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RunOptionPayload.ID,  RunOptionPayload.CODEC);

        // Receive option-click from client
        ServerPlayNetworking.registerGlobalReceiver(RunOptionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                for (ServerWorld world : context.server().getWorlds()) {
                    Entity entity = world.getEntity(payload.npcUuid());
                    if (entity instanceof DialogNpcEntity npc) {
                        List<DialogNpcEntity.DialogOption> opts = npc.getDialogOptions();
                        int idx = payload.optionIndex();
                        if (idx >= 0 && idx < opts.size()) {
                            String command = opts.get(idx).command();
                            context.server().getCommandManager().executeWithPrefix(
                                context.player().getCommandSource()
                                    .withSilent()
                                    .withLevel(2),
                                command
                            );
                        }
                        break;
                    }
                }
            });
        });
    }

    public static void registerClientPackets() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
            .registerGlobalReceiver(OpenDialogPayload.ID, (payload, context) ->
                context.client().execute(() ->
                    context.client().setScreen(
                        new com.example.dialognpc.screen.DialogScreen(
                            payload.npcUuid(),
                            payload.title(),
                            payload.text(),
                            payload.texture(),
                            payload.labels()
                        )
                    )
                )
            );
    }

    // ── Server helper: send dialog to player ─────────────────────────────

    public static void sendOpenDialog(ServerPlayerEntity player, DialogNpcEntity npc) {
        List<String> labels = npc.getDialogOptions().stream()
            .map(DialogNpcEntity.DialogOption::label)
            .toList();

        ServerPlayNetworking.send(player, new OpenDialogPayload(
            npc.getUuid(),
            npc.getDialogTitle(),
            npc.getDialogText(),
            npc.getNpcTexture(),
            labels
        ));
    }

    // ── Client helper: send option click to server ────────────────────────

    public static void sendRunOption(UUID npcUuid, int optionIndex) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
            .send(new RunOptionPayload(npcUuid, optionIndex));
    }
}
