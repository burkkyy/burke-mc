package burkemc.mixin;

import burkemc.managers.MenuManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;

        // Block THROW (Q key) on our item
        if (actionType == SlotActionType.THROW) {
            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                ItemStack stack = handler.slots.get(slotIndex).getStack();
                if (MenuManager.isMenuItem(stack)) {
                    ci.cancel();
                    handler.syncState();
                    return;
                }
            }
        }

        // Block PICKUP/CLONE/SWAP if our item is on the cursor or in the clicked slot
        if (actionType == SlotActionType.PICKUP ||
                actionType == SlotActionType.PICKUP_ALL ||
                actionType == SlotActionType.CLONE ||
                actionType == SlotActionType.SWAP) {

            ItemStack stack = handler.getCursorStack();

            if (MenuManager.isMenuItem(stack)) {
                ci.cancel();
                handler.syncState();
                return;
            }

            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                ItemStack stack2 = handler.slots.get(slotIndex).getStack();
                if (MenuManager.isMenuItem(stack2)) {
                    ci.cancel();
                    handler.syncState();
                    return;
                }
            }
        }
    }
}