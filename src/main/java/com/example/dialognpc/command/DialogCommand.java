package com.example.dialognpc.command;

import com.example.dialognpc.entity.DialogNpcEntity;
import com.example.dialognpc.entity.ModEntities;
import com.example.dialognpc.util.MinecraftColors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * All /npc commands, requiring OP level 2.
 *
 * Usage examples:
 *   /npc create "Nurse Joy"
 *   /npc settitle @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] "Nurse Joy"
 *   /npc settext  @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] "Would you like me to heal your party?"
 *   /npc addoption @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] "Heal Party" "pokeheal @p"
 *   /npc removeoption @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 0
 *   /npc clearoptions @e[type=dialognpc:dialog_npc,sort=nearest,limit=1]
 *   /npc settexture @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] dialognpc:textures/entity/nurse_joy.png
 *   /npc info @e[type=dialognpc:dialog_npc,sort=nearest,limit=1]
 *   /npc remove @e[type=dialognpc:dialog_npc,sort=nearest,limit=1]
 */
public class DialogCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("npc")
                .requires(src -> src.hasPermissionLevel(2))

                // /npc create <title>
                .then(CommandManager.literal("create")
                    .then(CommandManager.argument("title", StringArgumentType.string())
                        .executes(ctx -> createNpc(ctx,
                            StringArgumentType.getString(ctx, "title")))
                    )
                )

                // /npc settitle <entity> <title>
                .then(CommandManager.literal("settitle")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("title", StringArgumentType.greedyString())
                            .executes(ctx -> setTitle(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "title")))
                        )
                    )
                )

                // /npc settext <entity> <text>
                .then(CommandManager.literal("settext")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> setText(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "text")))
                        )
                    )
                )

                // /npc addoption <entity> <label> <command> [sound] [particle] [particleCount]
                // label: quotable string (use "Heal Party" for spaces)
                // command: greedy string (rest of line)
                // sound: optional sound ID (e.g., minecraft:entity.player.levelup)
                // particle: optional particle type (e.g., minecraft:happy_villager)
                // particleCount: number of particles (0-100)
                .then(CommandManager.literal("addoption")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("label", StringArgumentType.string())
                            .then(CommandManager.argument("command", StringArgumentType.string())
                                .executes(ctx -> addOption(ctx,
                                    EntityArgumentType.getEntity(ctx, "target"),
                                    StringArgumentType.getString(ctx, "label"),
                                    StringArgumentType.getString(ctx, "command"),
                                    null, null, 0))
                                .then(CommandManager.argument("sound", StringArgumentType.string())
                                    .suggests((ctx, builder) -> {
                                        net.minecraft.registry.Registries.SOUND_EVENT.getIndexedEntries().forEach(entry -> {
                                            entry.getKey().ifPresent(key -> builder.suggest(key.getValue().toString()));
                                        });
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> addOption(ctx,
                                        EntityArgumentType.getEntity(ctx, "target"),
                                        StringArgumentType.getString(ctx, "label"),
                                        StringArgumentType.getString(ctx, "command"),
                                        StringArgumentType.getString(ctx, "sound"),
                                        null, 0))
                                    .then(CommandManager.argument("particle", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            net.minecraft.registry.Registries.PARTICLE_TYPE.getIndexedEntries().forEach(entry -> {
                                                entry.getKey().ifPresent(key -> builder.suggest(key.getValue().toString()));
                                            });
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> addOption(ctx,
                                            EntityArgumentType.getEntity(ctx, "target"),
                                            StringArgumentType.getString(ctx, "label"),
                                            StringArgumentType.getString(ctx, "command"),
                                            StringArgumentType.getString(ctx, "sound"),
                                            StringArgumentType.getString(ctx, "particle"),
                                            5))
                                        .then(CommandManager.argument("particleCount", IntegerArgumentType.integer(0, 100))
                                            .executes(ctx -> addOption(ctx,
                                                EntityArgumentType.getEntity(ctx, "target"),
                                                StringArgumentType.getString(ctx, "label"),
                                                StringArgumentType.getString(ctx, "command"),
                                                StringArgumentType.getString(ctx, "sound"),
                                                StringArgumentType.getString(ctx, "particle"),
                                                IntegerArgumentType.getInteger(ctx, "particleCount")))
                                        )
                                    )
                                )
                            )
                        )
                    )
                )

                // /npc removeoption <entity> <index>
                .then(CommandManager.literal("removeoption")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                            .executes(ctx -> removeOption(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "index")))
                        )
                    )
                )

                // /npc clearoptions <entity>
                .then(CommandManager.literal("clearoptions")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .executes(ctx -> clearOptions(ctx,
                            EntityArgumentType.getEntity(ctx, "target")))
                    )
                )

                // /npc settexture <entity> <texture>
                // e.g.  dialognpc:textures/entity/nurse_joy.png
                //       minecraft:textures/entity/player/wide/alex.png
                .then(CommandManager.literal("settexture")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("texture", StringArgumentType.greedyString())
                            .executes(ctx -> setTexture(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "texture")))
                        )
                    )
                )

                // /npc setbgcolor <entity> <color>
                // Color format: 0xAARRGGBB (hex)
                .then(CommandManager.literal("setbgcolor")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("color", IntegerArgumentType.integer())
                            .executes(ctx -> setBackgroundColor(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "color")))
                        )
                    )
                )

                // /npc settitlecolor <entity> <color>
                // Color format: 0xAARRGGBB (hex)
                .then(CommandManager.literal("settitlecolor")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("color", IntegerArgumentType.integer())
                            .executes(ctx -> setTitleColor(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "color")))
                        )
                    )
                )

                // /npc setbtnwidth <entity> <width>
                .then(CommandManager.literal("setbtnwidth")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("width", IntegerArgumentType.integer(50, 400))
                            .executes(ctx -> setButtonWidth(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "width")))
                        )
                    )
                )

                // /npc setbordercolor <entity> <color>
                .then(CommandManager.literal("setbordercolor")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("color", IntegerArgumentType.integer())
                            .executes(ctx -> setBorderColor(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "color")))
                        )
                    )
                )

                // /npc settitletextcolor <entity> <color>
                .then(CommandManager.literal("settitletextcolor")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("color", IntegerArgumentType.integer())
                            .executes(ctx -> setTitleTextColor(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "color")))
                        )
                    )
                )

                // /npc setoptionsheight <entity> <height>
                .then(CommandManager.literal("setoptionsheight")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("height", IntegerArgumentType.integer(0, 500))
                            .executes(ctx -> setOptionsHeight(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "height")))
                        )
                    )
                )

                // Modal layout commands
                // /npc setboxwidth <entity> <width>
                .then(CommandManager.literal("setboxwidth")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("width", IntegerArgumentType.integer(200, 500))
                            .executes(ctx -> setBoxWidth(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "width")))
                        )
                    )
                )

                // /npc setboxheight <entity> <height> (0 = auto)
                .then(CommandManager.literal("setboxheight")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("height", IntegerArgumentType.integer(0, 500))
                            .executes(ctx -> setBoxHeight(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "height")))
                        )
                    )
                )

                // /npc settitleheight <entity> <height>
                .then(CommandManager.literal("settitleheight")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("height", IntegerArgumentType.integer(16, 48))
                            .executes(ctx -> setTitleHeight(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "height")))
                        )
                    )
                )

                // /npc setboxpadding <entity> <padding>
                .then(CommandManager.literal("setboxpadding")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("padding", IntegerArgumentType.integer(0, 30))
                            .executes(ctx -> setBoxPadding(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "padding")))
                        )
                    )
                )

                // /npc setportraitsize <entity> <size>
                .then(CommandManager.literal("setportraitsize")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("size", IntegerArgumentType.integer(16, 80))
                            .executes(ctx -> setPortraitSize(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "size")))
                        )
                    )
                )

                // /npc setheadtracking <entity> <true|false>
                .then(CommandManager.literal("setheadtracking")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("enabled", StringArgumentType.word())
                            .executes(ctx -> setHeadTracking(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "enabled")))
                        )
                    )
                )

                // /npc setbodyrotation <entity> <true|false>
                .then(CommandManager.literal("setbodyrotation")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("enabled", StringArgumentType.word())
                            .executes(ctx -> setBodyRotation(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "enabled")))
                        )
                    )
                )

                // /npc setcanmove <entity> <true|false>
                .then(CommandManager.literal("setcanmove")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("enabled", StringArgumentType.word())
                            .executes(ctx -> setCanMove(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "enabled")))
                        )
                    )
                )

                // /npc setcanrotate <entity> <true|false>
                .then(CommandManager.literal("setcanrotate")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("enabled", StringArgumentType.word())
                            .executes(ctx -> setCanRotate(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "enabled")))
                        )
                    )
                )

                // /npc sethitbox <entity> <true|false>
                .then(CommandManager.literal("sethitbox")
                    .then(CommandManager.argument("visible", StringArgumentType.word())
                        .executes(ctx -> setHitbox(ctx,
                            EntityArgumentType.getEntity(ctx, "target"),
                            StringArgumentType.getString(ctx, "visible")))
                    )
                )

                // /npc settexturetype <entity> <vanilla|player|url|base64>
                .then(CommandManager.literal("settexturetype")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("type", StringArgumentType.word())
                            .executes(ctx -> setTextureType(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "type")))
                        )
                    )
                )

                // /npc setcustomtexture <entity> <data>
                // For player: username
                // For url: full URL
                // For base64: base64 encoded image data
                .then(CommandManager.literal("setcustomtexture")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("data", StringArgumentType.greedyString())
                            .executes(ctx -> setCustomTexture(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "data")))
                        )
                    )
                )

                // /npc setname <entity> <name>
                // Sets the NPC's custom name (displayed above head in dialog)
                .then(CommandManager.literal("setname")
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> setNpcName(ctx,
                            EntityArgumentType.getEntity(ctx, "target"),
                            StringArgumentType.getString(ctx, "name")))
                    )
                )

                // /npc setcolor <entity> <colorName> [target: bg|title|border|titletext]
                // Color names: black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple,
                //              gold, gray, dark_gray, blue, green, aqua, red, light_purple,
                //              yellow, white
                // Target: bg (background), title (title bar), border, titletext (title text)
                // Default target is bg if not specified
                .then(CommandManager.literal("setcolor")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("colorName", StringArgumentType.word())
                            .executes(ctx -> setColor(ctx,
                                EntityArgumentType.getEntity(ctx, "target"),
                                StringArgumentType.getString(ctx, "colorName"),
                                "bg")
                            )
                            .then(CommandManager.argument("colorTarget", StringArgumentType.word())
                                .executes(ctx -> setColor(ctx,
                                    EntityArgumentType.getEntity(ctx, "target"),
                                    StringArgumentType.getString(ctx, "colorName"),
                                    StringArgumentType.getString(ctx, "colorTarget"))
                                )
                            )
                        )
                    )
                )

                // /npc info <entity>
                .then(CommandManager.literal("info")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .executes(ctx -> showInfo(ctx,
                            EntityArgumentType.getEntity(ctx, "target")))
                    )
                )

                // /npc remove <entity>
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .executes(ctx -> removeNpc(ctx,
                            EntityArgumentType.getEntity(ctx, "target")))
                    )
                )
        );
    }

    // ── Command implementations ────────────────────────────────────────────

    private static int createNpc(CommandContext<ServerCommandSource> ctx, String title)
            throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();

        DialogNpcEntity npc = ModEntities.DIALOG_NPC.create(player.getServerWorld());
        if (npc == null) {
            src.sendError(Text.literal("Failed to create NPC entity."));
            return 0;
        }

        npc.setDialogTitle(title);
        npc.refreshPositionAndAngles(
            player.getX(), player.getY(), player.getZ(),
            player.getYaw(), 0
        );
        player.getServerWorld().spawnEntity(npc);

        src.sendFeedback(
            () -> Text.literal("§a✔ Spawned NPC §e'" + title + "'§a. UUID: §7" + npc.getUuidAsString()
                + "\n§7Tip: Use §f@e[type=dialognpc:dialog_npc,sort=nearest,limit=1]§7 to target it."),
            true
        );
        return 1;
    }

    private static int setTitle(CommandContext<ServerCommandSource> ctx, Entity entity, String title) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setDialogTitle(title);
        ctx.getSource().sendFeedback(() -> Text.literal("§aTitle set to: §e" + title), false);
        return 1;
    }

    private static int setText(CommandContext<ServerCommandSource> ctx, Entity entity, String text) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setDialogText(text);
        ctx.getSource().sendFeedback(() -> Text.literal("§aDialog text updated."), false);
        return 1;
    }

    private static int addOption(CommandContext<ServerCommandSource> ctx, Entity entity,
                                  String label, String command, String sound, String particle, int particleCount) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;

        // Validate sound if provided
        String validSound = sound;
        if (sound != null && !sound.isEmpty()) {
            var soundId = net.minecraft.util.Identifier.tryParse(sound);
            if (soundId == null || net.minecraft.registry.Registries.SOUND_EVENT.get(soundId) == null) {
                ctx.getSource().sendError(Text.literal("§cInvalid sound ID: §7" + sound));
                return 0;
            }
        }

        // Validate particle if provided
        String validParticle = particle;
        if (particle != null && !particle.isEmpty()) {
            var particleId = net.minecraft.util.Identifier.tryParse(particle);
            if (particleId == null || net.minecraft.registry.Registries.PARTICLE_TYPE.get(particleId) == null) {
                ctx.getSource().sendError(Text.literal("§cInvalid particle type: §7" + particle));
                return 0;
            }
        }

        npc.addDialogOption(new DialogNpcEntity.DialogOption(label, command, validSound, validParticle, particleCount));
        int idx = npc.getDialogOptions().size() - 1;
        StringBuilder feedback = new StringBuilder("§aOption added at index §e" + idx + "§a: §f[" + label + "] §7→ " + command);
        if (validSound != null && !validSound.isEmpty()) feedback.append(" §7| Sound: §e").append(validSound);
        if (validParticle != null && !validParticle.isEmpty()) feedback.append(" §7| Particles: §e").append(validParticle).append(" §7(§e").append(particleCount).append("§7)");
        ctx.getSource().sendFeedback(() -> Text.literal(feedback.toString()), false);
        return 1;
    }

    private static int removeOption(CommandContext<ServerCommandSource> ctx, Entity entity, int index) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        List<DialogNpcEntity.DialogOption> opts = npc.getDialogOptions();
        if (index < 0 || index >= opts.size()) {
            ctx.getSource().sendError(Text.literal("Index " + index + " out of range (0-" + (opts.size() - 1) + ")."));
            return 0;
        }
        opts.remove(index);
        ctx.getSource().sendFeedback(() -> Text.literal("§aOption " + index + " removed."), false);
        return 1;
    }

    private static int clearOptions(CommandContext<ServerCommandSource> ctx, Entity entity) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.clearDialogOptions();
        ctx.getSource().sendFeedback(() -> Text.literal("§aAll options cleared."), false);
        return 1;
    }

    private static int setTexture(CommandContext<ServerCommandSource> ctx, Entity entity, String texture) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;

        // Auto-detect texture type based on the texture string format
        String detectedType = "vanilla";
        String customData = "";

        if (texture.startsWith("http://") || texture.startsWith("https://")) {
            // URL texture
            detectedType = "url";
            customData = texture;
        } else if (texture.startsWith("data:image")) {
            // Base64 data URI
            detectedType = "base64";
            customData = texture;
        } else if (texture.length() > 0 && !texture.contains(":") && !texture.contains("/")) {
            // Likely a player username (no colons or slashes)
            // Check if it looks like a Minecraft username (3-16 chars, alphanumeric + underscores)
            if (texture.matches("^[a-zA-Z0-9_]{3,16}$")) {
                detectedType = "player";
                customData = texture;
            }
        }

        final String finalType = detectedType;
        npc.setTextureType(detectedType);
        if (!customData.isEmpty()) {
            npc.setCustomTextureData(customData);
        } else {
            npc.setNpcTexture(texture);
        }

        ctx.getSource().sendFeedback(() -> Text.literal("§aTexture set to: §7" + texture + " §7(type: §e" + finalType + "§7)"), false);
        return 1;
    }

    private static int setBackgroundColor(CommandContext<ServerCommandSource> ctx, Entity entity, int color) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setBackgroundColor(color);
        ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aBackground color set to: §70x%08X", color)), false);
        return 1;
    }

    private static int setTitleColor(CommandContext<ServerCommandSource> ctx, Entity entity, int color) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setTitleColor(color);
        ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aTitle bar color set to: §70x%08X", color)), false);
        return 1;
    }

    private static int setButtonWidth(CommandContext<ServerCommandSource> ctx, Entity entity, int width) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setButtonWidth(width);
        ctx.getSource().sendFeedback(() -> Text.literal("§aButton width set to: §e" + width), false);
        return 1;
    }

    private static int setBorderColor(CommandContext<ServerCommandSource> ctx, Entity entity, int color) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setBorderColor(color);
        ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aBorder color set to: §70x%08X", color)), false);
        return 1;
    }

    private static int setTitleTextColor(CommandContext<ServerCommandSource> ctx, Entity entity, int color) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setTitleTextColor(color);
        ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aTitle text color set to: §70x%08X", color)), false);
        return 1;
    }

    private static int setOptionsHeight(CommandContext<ServerCommandSource> ctx, Entity entity, int height) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setOptionsHeight(height);
        ctx.getSource().sendFeedback(() -> Text.literal("§aOptions area height set to: §e" + height + (height == 0 ? " (auto)" : "px")), false);
        return 1;
    }

    private static int setBoxWidth(CommandContext<ServerCommandSource> ctx, Entity entity, int width) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setBoxWidth(width);
        ctx.getSource().sendFeedback(() -> Text.literal("§aDialog box width set to: §e" + width + "px"), false);
        return 1;
    }

    private static int setBoxHeight(CommandContext<ServerCommandSource> ctx, Entity entity, int height) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setBoxHeight(height);
        ctx.getSource().sendFeedback(() -> Text.literal("§aDialog box height set to: §e" + height + (height == 0 ? " (auto)" : "px")), false);
        return 1;
    }

    private static int setTitleHeight(CommandContext<ServerCommandSource> ctx, Entity entity, int height) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setTitleHeight(height);
        ctx.getSource().sendFeedback(() -> Text.literal("§aTitle bar height set to: §e" + height + "px"), false);
        return 1;
    }

    private static int setBoxPadding(CommandContext<ServerCommandSource> ctx, Entity entity, int padding) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setBoxPadding(padding);
        ctx.getSource().sendFeedback(() -> Text.literal("§aDialog box padding set to: §e" + padding + "px"), false);
        return 1;
    }

    private static int setPortraitSize(CommandContext<ServerCommandSource> ctx, Entity entity, int size) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setPortraitSize(size);
        ctx.getSource().sendFeedback(() -> Text.literal("§aPortrait size set to: §e" + size + "x" + size), false);
        return 1;
    }

    private static int setHeadTracking(CommandContext<ServerCommandSource> ctx, Entity entity, String enabled) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        boolean value = Boolean.parseBoolean(enabled);
        npc.setHeadTracking(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aHead tracking " + (value ? "§eenabled" : "§cdisabled")), false);
        return 1;
    }

    private static int setBodyRotation(CommandContext<ServerCommandSource> ctx, Entity entity, String enabled) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        boolean value = Boolean.parseBoolean(enabled);
        npc.setBodyRotation(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aBody rotation " + (value ? "§eenabled" : "§cdisabled")), false);
        return 1;
    }

    private static int setCanMove(CommandContext<ServerCommandSource> ctx, Entity entity, String enabled) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        boolean value = Boolean.parseBoolean(enabled);
        npc.setCanMove(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aMovement " + (value ? "§eenabled" : "§cdisabled")), false);
        return 1;
    }

    private static int setCanRotate(CommandContext<ServerCommandSource> ctx, Entity entity, String enabled) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        boolean value = Boolean.parseBoolean(enabled);
        npc.setCanRotate(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aRotation " + (value ? "§eenabled" : "§cdisabled")), false);
        return 1;
    }

    private static int setHitbox(CommandContext<ServerCommandSource> ctx, Entity entity, String visible) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        boolean value = Boolean.parseBoolean(visible);
        npc.setShowHitbox(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aHitbox " + (value ? "§eshown" : "§chidden")), false);
        return 1;
    }

    private static int setTextureType(CommandContext<ServerCommandSource> ctx, Entity entity, String type) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        String validType = switch (type.toLowerCase()) {
            case "vanilla", "player", "url", "base64" -> type.toLowerCase();
            default -> {
                ctx.getSource().sendError(Text.literal("Invalid texture type. Use: vanilla, player, url, or base64"));
                yield "vanilla";
            }
        };
        npc.setTextureType(validType);
        ctx.getSource().sendFeedback(() -> Text.literal("§aTexture type set to: §e" + validType), false);
        return 1;
    }

    private static int setCustomTexture(CommandContext<ServerCommandSource> ctx, Entity entity, String data) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setCustomTextureData(data);
        ctx.getSource().sendFeedback(() -> Text.literal("§aCustom texture data set: §7" + (data.length() > 50 ? data.substring(0, 50) + "..." : data)), false);
        return 1;
    }

    private static int setNpcName(CommandContext<ServerCommandSource> ctx, Entity entity, String name) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.setCustomName(Text.literal(name));
        ctx.getSource().sendFeedback(() -> Text.literal("§aNPC name set to: §e" + name), false);
        return 1;
    }

    private static int setColor(CommandContext<ServerCommandSource> ctx, Entity entity, String colorName, String colorTarget) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;

        if (!MinecraftColors.isValidColorName(colorName)) {
            ctx.getSource().sendError(Text.literal("§cInvalid color name. Valid colors: " + String.join(", ", MinecraftColors.getColorNames())));
            return 0;
        }

        int color = MinecraftColors.getColor(colorName);
        colorTarget = colorTarget.toLowerCase();

        switch (colorTarget) {
            case "bg", "background" -> {
                npc.setBackgroundColor(color);
                ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aBackground color set to: §e%s §7(0x%08X)", colorName, color)), false);
            }
            case "title", "titlebar" -> {
                npc.setTitleColor(color);
                ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aTitle bar color set to: §e%s §7(0x%08X)", colorName, color)), false);
            }
            case "border" -> {
                npc.setBorderColor(color);
                ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aBorder color set to: §e%s §7(0x%08X)", colorName, color)), false);
            }
            case "titletext", "titletextcolor" -> {
                npc.setTitleTextColor(color);
                ctx.getSource().sendFeedback(() -> Text.literal(String.format("§aTitle text color set to: §e%s §7(0x%08X)", colorName, color)), false);
            }
            default -> {
                ctx.getSource().sendError(Text.literal("§cInvalid target. Use: bg, title, border, or titletext"));
                return 0;
            }
        }
        return 1;
    }

    private static int showInfo(CommandContext<ServerCommandSource> ctx, Entity entity) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Dialog NPC Info ===\n");
        sb.append("§7UUID:          §f").append(npc.getUuidAsString()).append("\n");
        sb.append("§7Title:         §e").append(npc.getDialogTitle()).append("\n");
        sb.append("§7Text:          §f").append(npc.getDialogText()).append("\n");
        sb.append("§7Name:          §e").append(npc.getCustomName() != null ? npc.getCustomName().getString() : "(none)").append("\n");
        sb.append("§7Texture:       §f").append(npc.getNpcTexture()).append("\n");
        sb.append(String.format("§7BG Color:      §f0x%08X\n", npc.getBackgroundColor()));
        sb.append(String.format("§7Title Bar:     §f0x%08X\n", npc.getTitleColor()));
        sb.append(String.format("§7Title Text:    §f0x%08X\n", npc.getTitleTextColor()));
        sb.append(String.format("§7Border:        §f0x%08X\n", npc.getBorderColor()));
        sb.append("§7Btn Width:     §e").append(npc.getButtonWidth()).append("\n");
        sb.append("§7Options Height:§e").append(npc.getOptionsHeight() == 0 ? "auto" : npc.getOptionsHeight() + "px").append("\n");
        // Modal layout
        sb.append("§7Box Width:     §e").append(npc.getBoxWidth()).append("px\n");
        sb.append("§7Box Height:    §e").append(npc.getBoxHeight() == 0 ? "auto" : npc.getBoxHeight() + "px").append("\n");
        sb.append("§7Title Height:  §e").append(npc.getTitleHeight()).append("px\n");
        sb.append("§7Box Padding:   §e").append(npc.getBoxPadding()).append("px\n");
        sb.append("§7Portrait Size: §e").append(npc.getPortraitSize()).append("x").append(npc.getPortraitSize()).append("\n");
        // Behavior flags
        sb.append("§7Head Tracking: ").append(npc.isHeadTracking() ? "§aYes" : "§cNo").append("\n");
        sb.append("§7Body Rotation: ").append(npc.isBodyRotation() ? "§aYes" : "§cNo").append("\n");
        sb.append("§7Can Move:      ").append(npc.isCanMove() ? "§aYes" : "§cNo").append("\n");
        sb.append("§7Can Rotate:    ").append(npc.isCanRotate() ? "§aYes" : "§cNo").append("\n");
        sb.append("§7Show Hitbox:   ").append(npc.isShowHitbox() ? "§aYes" : "§cNo").append("\n");
        // Custom texture
        sb.append("§7Texture Type:  §e").append(npc.getTextureType()).append("\n");
        if (!npc.getCustomTextureData().isEmpty()) {
            String data = npc.getCustomTextureData();
            sb.append("§7Custom Data:   §f").append(data.length() > 40 ? data.substring(0, 40) + "..." : data).append("\n");
        }
        sb.append("§7Options (").append(npc.getDialogOptions().size()).append("):\n");
        for (int i = 0; i < npc.getDialogOptions().size(); i++) {
            DialogNpcEntity.DialogOption opt = npc.getDialogOptions().get(i);
            sb.append("  §e").append(i).append("§7: §f[").append(opt.label())
              .append("] §7→ ").append(opt.command()).append("\n");
        }

        String info = sb.toString();
        ctx.getSource().sendFeedback(() -> Text.literal(info), false);
        return 1;
    }

    private static int removeNpc(CommandContext<ServerCommandSource> ctx, Entity entity) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.discard();
        ctx.getSource().sendFeedback(() -> Text.literal("§aNPC removed."), true);
        return 1;
    }

    // ── Utility ──────────────────────────────────────────────────────────

    private static DialogNpcEntity asNpc(CommandContext<ServerCommandSource> ctx, Entity entity) {
        if (entity instanceof DialogNpcEntity npc) return npc;
        ctx.getSource().sendError(Text.literal("That entity is not a Dialog NPC."));
        return null;
    }
}
