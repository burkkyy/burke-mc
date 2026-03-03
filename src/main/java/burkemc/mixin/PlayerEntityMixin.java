package burkemc.mixin;

import burkemc.menu.MenuManager;
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
            ItemStack stack = player.getInventory().getStack(MenuManager.MENU_ITEM_SLOT);
            if (stack.isOf(MenuManager.MENU_ITEM)) {
                player.getInventory().setStack(MenuManager.MENU_ITEM_SLOT, ItemStack.EMPTY);
            }
        }
    }
}