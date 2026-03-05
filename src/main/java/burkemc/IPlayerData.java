package burkemc;

import net.minecraft.server.network.ServerPlayerEntity;

public interface IPlayerData {
    PlayerSettings getBurkeMcSettings();

    void save();

    static IPlayerData of(ServerPlayerEntity player) {
        return (IPlayerData) player;
    }
}
