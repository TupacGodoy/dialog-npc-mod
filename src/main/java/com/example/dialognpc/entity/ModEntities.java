package com.example.dialognpc.entity;

import com.example.dialognpc.DialogNpcMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<DialogNpcEntity> DIALOG_NPC = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(DialogNpcMod.MOD_ID, "dialog_npc"),
        EntityType.Builder.<DialogNpcEntity>create(DialogNpcEntity::new, SpawnGroup.MISC)
            .dimensions(0.6F, 1.8F)
            .maxTrackingRange(64)
            .trackingTickInterval(3)
            .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(DIALOG_NPC, DialogNpcEntity.createAttributes());
    }
}
