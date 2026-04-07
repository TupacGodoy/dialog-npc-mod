package com.example.dialognpc.network;

import com.example.dialognpc.DialogNpcMod;
import com.example.dialognpc.entity.DialogNpcEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
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
        String titleKey,
        String text,
        String textKey,
        String texture,
        String textureType,
        String customTextureData,
        String npcName,
        String npcNameKey,
        List<String> labels,
        List<String> labelKeys,
        List<String> sounds,
        List<String> particles,
        List<Integer> particleCounts,
        int    backgroundColor,
        int    titleColor,
        int    buttonWidth,
        int    borderColor,
        int    titleTextColor,
        int    optionsHeight,
        int    boxWidth,
        int    boxHeight,
        int    titleHeight,
        int    boxPadding,
        int    portraitSize
    ) implements CustomPayload {

        public static final CustomPayload.Id<OpenDialogPayload> ID =
            new CustomPayload.Id<>(Identifier.of(DialogNpcMod.MOD_ID, "open_dialog"));

        public static final PacketCodec<PacketByteBuf, OpenDialogPayload> CODEC =
            new PacketCodec<>() {
                @Override
                public OpenDialogPayload decode(PacketByteBuf buf) {
                    UUID   uuid    = buf.readUuid();
                    String title   = buf.readString();
                    String titleKey = buf.readString();
                    String text    = buf.readString();
                    String textKey = buf.readString();
                    String texture = buf.readString();
                    String textureType = buf.readString();
                    String customTextureData = buf.readString();
                    String npcName = buf.readString();
                    String npcNameKey = buf.readString();
                    int count = buf.readVarInt();
                    List<String> labels = new ArrayList<>(count);
                    List<String> labelKeys = new ArrayList<>(count);
                    List<String> sounds = new ArrayList<>(count);
                    List<String> particles = new ArrayList<>(count);
                    List<Integer> particleCounts = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        labels.add(buf.readString());
                        labelKeys.add(buf.readString());
                        sounds.add(buf.readBoolean() ? buf.readString() : "");
                        particles.add(buf.readBoolean() ? buf.readString() : "");
                        particleCounts.add(buf.readVarInt());
                    }
                    int bgColor = buf.readInt();
                    int titleColor = buf.readInt();
                    int btnWidth = buf.readInt();
                    int borderColor = buf.readInt();
                    int titleTextColor = buf.readInt();
                    int optionsHeight = buf.readInt();
                    int boxWidth = buf.readInt();
                    int boxHeight = buf.readInt();
                    int titleHeight = buf.readInt();
                    int boxPadding = buf.readInt();
                    int portraitSize = buf.readInt();
                    return new OpenDialogPayload(uuid, title, titleKey, text, textKey, texture, textureType, customTextureData, npcName, npcNameKey, labels, labelKeys, sounds, particles, particleCounts, bgColor, titleColor, btnWidth, borderColor, titleTextColor, optionsHeight, boxWidth, boxHeight, titleHeight, boxPadding, portraitSize);
                }

                @Override
                public void encode(PacketByteBuf buf, OpenDialogPayload p) {
                    buf.writeUuid(p.npcUuid());
                    buf.writeString(p.title());
                    buf.writeString(p.titleKey());
                    buf.writeString(p.text());
                    buf.writeString(p.textKey());
                    buf.writeString(p.texture());
                    buf.writeString(p.textureType());
                    buf.writeString(p.customTextureData());
                    buf.writeString(p.npcName());
                    buf.writeString(p.npcNameKey());
                    buf.writeVarInt(p.labels().size());
                    for (int i = 0; i < p.labels().size(); i++) {
                        buf.writeString(p.labels().get(i));
                        buf.writeString(p.labelKeys().get(i));
                        buf.writeBoolean(p.sounds().get(i) != null && !p.sounds().get(i).isEmpty());
                        if (p.sounds().get(i) != null && !p.sounds().get(i).isEmpty()) buf.writeString(p.sounds().get(i));
                        buf.writeBoolean(p.particles().get(i) != null && !p.particles().get(i).isEmpty());
                        if (p.particles().get(i) != null && !p.particles().get(i).isEmpty()) buf.writeString(p.particles().get(i));
                        buf.writeVarInt(p.particleCounts().get(i));
                    }
                    buf.writeInt(p.backgroundColor());
                    buf.writeInt(p.titleColor());
                    buf.writeInt(p.buttonWidth());
                    buf.writeInt(p.borderColor());
                    buf.writeInt(p.titleTextColor());
                    buf.writeInt(p.optionsHeight());
                    buf.writeInt(p.boxWidth());
                    buf.writeInt(p.boxHeight());
                    buf.writeInt(p.titleHeight());
                    buf.writeInt(p.boxPadding());
                    buf.writeInt(p.portraitSize());
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
        ClientPacketHandler.register();
    }

    // ── Server helper: send dialog to player ─────────────────────────────

    public static void sendOpenDialog(ServerPlayerEntity player, DialogNpcEntity npc) {
        List<DialogNpcEntity.DialogOption> opts = npc.getDialogOptions();
        List<String> labels = opts.stream().map(DialogNpcEntity.DialogOption::label).toList();
        List<String> labelKeys = opts.stream().map(o -> o.labelTranslationKey() != null ? o.labelTranslationKey() : "").toList();
        List<String> sounds = opts.stream().map(o -> o.soundId() != null ? o.soundId() : "").toList();
        List<String> particles = opts.stream().map(o -> o.particleType() != null ? o.particleType() : "").toList();
        List<Integer> particleCounts = opts.stream().map(DialogNpcEntity.DialogOption::particleCount).toList();

        ServerPlayNetworking.send(player, new OpenDialogPayload(
            npc.getUuid(),
            npc.getDialogTitle(),
            npc.getDialogTitleKey(),
            npc.getDialogText(),
            npc.getDialogTextKey(),
            npc.getNpcTexture(),
            npc.getTextureType(),
            npc.getCustomTextureData(),
            npc.getCustomName() != null ? npc.getCustomName().getString() : "",
            npc.getNpcNameKey(),
            labels,
            labelKeys,
            sounds,
            particles,
            particleCounts,
            npc.getBackgroundColor(),
            npc.getTitleColor(),
            npc.getButtonWidth(),
            npc.getBorderColor(),
            npc.getTitleTextColor(),
            npc.getOptionsHeight(),
            npc.getBoxWidth(),
            npc.getBoxHeight(),
            npc.getTitleHeight(),
            npc.getBoxPadding(),
            npc.getPortraitSize()
        ));
    }

    // ── Client helper: send option click to server ────────────────────────

    public static void sendRunOption(UUID npcUuid, int optionIndex) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
            .send(new RunOptionPayload(npcUuid, optionIndex));
    }
}
