package burkemc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Burkemc implements ModInitializer {
	public static final String MOD_ID = "burke-mc";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			handler.player.sendSystemMessage(
					Component.literal("Welcome to the Burke MC Server!").withStyle(ChatFormatting.RED)
			);
		});

		LOGGER.info("Hello Fabric world!");
	}
}