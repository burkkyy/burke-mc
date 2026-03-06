package burkemc.mixin;

import burkemc.player.IPlayerData;
import burkemc.player.PlayerSettings;
import burkemc.player.PlayerWallet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements IPlayerData {
    @Unique
    private PlayerSettings settings = new PlayerSettings();

    @Unique
    private PlayerWallet wallet = new PlayerWallet();

    @Override
    public void save(){
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        var server = self.getEntityWorld().getServer();
        if (server == null) return;
        ((PlayerManagerAccessor) server.getPlayerManager()).invokeSavePlayerData(self);
    }

    @Override
    public PlayerSettings getSettings() { return settings; }

    @Override
    public PlayerWallet getWallet() { return wallet; }

    @Inject(method = "readCustomData", at = @At("RETURN"))
    private void readCustomData(ReadView view, CallbackInfo ci) {
        this.settings = PlayerSettings.readFrom(view);
        this.wallet = PlayerWallet.readFrom(view);
    }

    @Inject(method = "writeCustomData", at = @At("RETURN"))
    private void writeCustomData(WriteView view, CallbackInfo ci) {
        this.settings.writeTo(view);
        this.wallet.writeTo(view);
    }
}
