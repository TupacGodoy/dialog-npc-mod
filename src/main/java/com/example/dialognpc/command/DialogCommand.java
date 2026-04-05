package com.example.dialognpc.command;

import com.example.dialognpc.entity.DialogNpcEntity;
import com.example.dialognpc.entity.ModEntities;
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

                // /npc addoption <entity> <label> <command>
                // label: quotable string (use "Heal Party" for spaces)
                // command: greedy string (rest of line)
                .then(CommandManager.literal("addoption")
                    .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("label", StringArgumentType.string())
                            .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> addOption(ctx,
                                    EntityArgumentType.getEntity(ctx, "target"),
                                    StringArgumentType.getString(ctx, "label"),
                                    StringArgumentType.getString(ctx, "command")))
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
                                  String label, String command) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;
        npc.addDialogOption(new DialogNpcEntity.DialogOption(label, command));
        int idx = npc.getDialogOptions().size() - 1;
        ctx.getSource().sendFeedback(
            () -> Text.literal("§aOption added at index §e" + idx
                + "§a: §f[" + label + "] §7→ " + command),
            false
        );
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
        npc.setNpcTexture(texture);
        ctx.getSource().sendFeedback(() -> Text.literal("§aTexture set to: §7" + texture), false);
        return 1;
    }

    private static int showInfo(CommandContext<ServerCommandSource> ctx, Entity entity) {
        DialogNpcEntity npc = asNpc(ctx, entity);
        if (npc == null) return 0;

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Dialog NPC Info ===\n");
        sb.append("§7UUID:    §f").append(npc.getUuidAsString()).append("\n");
        sb.append("§7Title:   §e").append(npc.getDialogTitle()).append("\n");
        sb.append("§7Text:    §f").append(npc.getDialogText()).append("\n");
        sb.append("§7Texture: §f").append(npc.getNpcTexture()).append("\n");
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
