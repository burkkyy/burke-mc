package burkemc;

import burkemc.block.BurkeMcBlocks;
import burkemc.command.CommandRegistry;
import burkemc.item.BurkeMcItems;

import burkemc.menu.MainMenuManager;
import burkemc.player.PlayerHud;
import burkemc.recipe.BurkeMcRecipeLoader;
import burkemc.util.TickScheduler;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
    public static final String MOD_ID = "burkemc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        TickScheduler.register();
        CommandRegistry.register();
        MainMenuManager.register();

        // Order here matters
        ResourceManagerHelperImpl.get(ResourceType.SERVER_DATA).registerReloadListener(new BurkeMcRecipeLoader());
        BurkeMcItems.initialize();
        BurkeMcBlocks.initialize();

        var result = PolymerResourcePackUtils.addModAssets(MOD_ID);
        LOGGER.info("PolymerResourcePackUtils.addModAssets={}", result);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            server.execute(() -> {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.changeGameMode(GameMode.SURVIVAL);
                }

                PlayerHud.initialize(handler.player);
            });
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                var server = player.getEntityWorld().getServer();

                if (server != null) {
                    player.changeGameMode(GameMode.SPECTATOR);

                    TickScheduler.schedule(() -> {
                        BannedPlayerList banList = server.getPlayerManager().getUserBanList();

                        long banDurationMs = 36 * 60 * 60 * 1000; // 36 hours
                        Date expiryDate = new Date(System.currentTimeMillis() + banDurationMs);
                        String reason = "You died! Banned for 2 days.";

                        var profile = player.getPlayerConfigEntry();

                        BannedPlayerEntry banEntry = new BannedPlayerEntry(profile, new Date(), "Death System", expiryDate, reason);

                        banList.add(banEntry);
                        player.networkHandler.disconnect(Text.literal(reason));

                        LOGGER.info("Banned player {} for 2 days due to death.", player.getEntity());
                    }, 20);
                }
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                TickScheduler.schedule(() -> newPlayer.sendMessage(Text.literal("Respawning in 5")), 20);
                TickScheduler.schedule(() -> newPlayer.sendMessage(Text.literal("4")), 40);
                TickScheduler.schedule(() -> newPlayer.sendMessage(Text.literal("3")), 60);
                TickScheduler.schedule(() -> newPlayer.sendMessage(Text.literal("2")), 80);
                TickScheduler.schedule(() -> newPlayer.sendMessage(Text.literal("1")), 100);
                TickScheduler.schedule(() -> newPlayer.changeGameMode(GameMode.SURVIVAL), 120);
            }
        });
    }
}