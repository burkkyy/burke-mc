package burkemc.mixin;

import burkemc.menu.MainMenuManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "dropInventory", at = @At("HEAD"))
    private void onDropInventory(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        if (player instanceof ServerPlayerEntity) {
            ItemStack stack = player.getInventory().getStack(MainMenuManager.MENU_ITEM_SLOT);
            if (stack.isOf(MainMenuManager.MENU_ITEM)) {
                player.getInventory().setStack(MainMenuManager.MENU_ITEM_SLOT, ItemStack.EMPTY);
            }
        }
    }
}