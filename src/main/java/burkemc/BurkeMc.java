package burkemc;

import com.mojang.authlib.GameProfile;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class BurkeMc implements ModInitializer {
	public static final String MOD_ID = "burke-mc";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity player) {
				var server = player.getEntityWorld().getServer();

				if(server != null){
					BannedPlayerList banList = server.getPlayerManager().getUserBanList();

					long banDurationMs = 2L * 24 * 60 * 60 * 1000;	// 2 days
					Date expiryDate = new Date(System.currentTimeMillis() + banDurationMs);
					String reason = "You died! Banned for 2 days.";

					var profile = player.getPlayerConfigEntry();

					BannedPlayerEntry banEntry = new BannedPlayerEntry(profile, new Date(), "Death System", expiryDate, reason);

					banList.add(banEntry);
					player.networkHandler.disconnect(Text.literal(reason));

					LOGGER.info("Banned player {} for 2 days due to death.", player.getEntity());
				}
			}
		});

		LOGGER.info("Hello Fabric world!");
	}
}