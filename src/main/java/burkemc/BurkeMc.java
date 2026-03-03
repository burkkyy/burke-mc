package burkemc;

import burkemc.item.BurkeMcItems;
import burkemc.menu.MenuManager;

import burkemc.recipe.BurkeMcRecipeLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            server.execute(() -> {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.setHealth(player.getMaxHealth());
                    player.changeGameMode(GameMode.SURVIVAL);
                }
            });
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                var server = player.getEntityWorld().getServer();

                if (server != null) {
                    player.changeGameMode(GameMode.SPECTATOR);
                    server.execute(() -> {
                        BannedPlayerList banList = server.getPlayerManager().getUserBanList();

                        long banDurationMs = 36 * 60 * 60 * 1000; // 36 hours
                        Date expiryDate = new Date(System.currentTimeMillis() + banDurationMs);
                        String reason = "You died! Banned for 2 days.";

                        var profile = player.getPlayerConfigEntry();

                        BannedPlayerEntry banEntry = new BannedPlayerEntry(profile, new Date(), "Death System", expiryDate, reason);

                        banList.add(banEntry);
                        player.networkHandler.disconnect(Text.literal(reason));

                        LOGGER.info("Banned player {} for 2 days due to death.", player.getEntity());
                    });
                }
            }
        });

        MenuManager.register();

        ResourceManagerHelperImpl.get(ResourceType.SERVER_DATA).registerReloadListener(new BurkeMcRecipeLoader());
        BurkeMcItems.initialize();

        LOGGER.info("Hello Fabric world!");
    }
}