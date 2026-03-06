package burkemc.player;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Any changes should be reflected in src/main/java/burkemc/mixin/ServerPlayerEntityMixin.java
 */
public interface IPlayerData {
    PlayerWallet getWallet();
    PlayerSettings getSettings();

    void save();

    static IPlayerData of(ServerPlayerEntity player) {
        return (IPlayerData) player;
    }
}
