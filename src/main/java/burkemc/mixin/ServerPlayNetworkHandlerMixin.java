package burkemc.mixin;

import burkemc.managers.MenuManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onHandSwing", at = @At("HEAD"))
    private void onHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        if (MenuManager.isMenuItem(player.getStackInHand(packet.getHand()))) {
            MenuManager.openMainMenu(player);
        }
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void onCustomClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        // 0. Block any THROW (Q key) action targeting our item
        if (packet.actionType() == SlotActionType.THROW) {
            int slotIndex = packet.slot();
            if (slotIndex >= 0 && slotIndex < player.currentScreenHandler.slots.size()) {
                if (MenuManager.isMenuItem(player.currentScreenHandler.getSlot(slotIndex).getStack())) {
                    cancelAndSync(ci);
                    return;
                }
            }
        }

        // 1. Block hotkey swapping (number keys 1-9 in inventory)
        if (packet.actionType() == SlotActionType.SWAP) {
            // Check the hotbar slot being swapped TO
            ItemStack hotbarStack = player.getInventory().getStack(packet.button());
            if (MenuManager.isMenuItem(hotbarStack)) {
                cancelAndSync(ci);
                return;
            }
            // Also check the slot being swapped FROM
            int slotIndex = packet.slot();
            if (slotIndex >= 0 && slotIndex < player.currentScreenHandler.slots.size()) {
                if (MenuManager.isMenuItem(player.currentScreenHandler.getSlot(slotIndex).getStack())) {
                    cancelAndSync(ci);
                    return;
                }
            }
        }

        // 2. Check cursor
        if (MenuManager.isMenuItem(player.currentScreenHandler.getCursorStack())) {
            cancelAndSync(ci);
            return;
        }

        // 3. Check clicked slot
        int slotIndex = packet.slot();
        if (slotIndex >= 0 && slotIndex < player.currentScreenHandler.slots.size()) {
            if (MenuManager.isMenuItem(player.currentScreenHandler.getSlot(slotIndex).getStack())) {
                cancelAndSync(ci);
            }
        }
    }

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void onCustomPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        PlayerActionC2SPacket.Action action = packet.getAction();

        if (action == PlayerActionC2SPacket.Action.DROP_ITEM ||
                action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {

            ItemStack selected = player.getInventory().getStack(player.getInventory().getSelectedSlot());
            if (MenuManager.isMenuItem(selected) ||
                    MenuManager.isMenuItem(player.getMainHandStack()) ||
                    MenuManager.isMenuItem(player.getOffHandStack())) {

                ci.cancel();
                player.currentScreenHandler.syncState();
                player.currentScreenHandler.sendContentUpdates();
            }
        }
    }

    @Unique
    private void cancelAndSync(CallbackInfo ci) {
        ci.cancel();
        player.currentScreenHandler.syncState();
    }
}
