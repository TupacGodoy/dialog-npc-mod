package com.example.dialognpc;

import com.example.dialognpc.command.DialogCommand;
import com.example.dialognpc.entity.ModEntities;
import com.example.dialognpc.network.ModPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogNpcMod implements ModInitializer {

    public static final String MOD_ID = "dialognpc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModPackets.registerServerPackets();   // also registers payload types (both S2C and C2S)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            DialogCommand.register(dispatcher)
        );
        LOGGER.info("Dialog NPC mod initialized.");
    }
}
