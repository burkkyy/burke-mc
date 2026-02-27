package burkemc.gui;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

public class NavigableMenuHandler extends GenericContainerScreenHandler {
    private final ServerPlayerEntity player;

    public NavigableMenuHandler(int syncId, PlayerInventory playerInventory,
                                Inventory inventory, ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
        this.player = player;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 54) {
            ItemStack clicked = getSlot(slotIndex).getStack();
            handleClick(clicked);
            return;
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    private void handleClick(ItemStack stack) {
        if (stack.isEmpty()) return;

        var x = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().get("MenuAction");

//        System.out.println(x);

//        switch (action) {
//            case "open_submenu_1" -> {
//                player.closeHandledScreen();
//                // Delay by 1 tick so the close packet processes first
//                player.getServer().execute(() -> MenuManager.openSubmenu1(player));
//            }
//            case "open_submenu_2" -> {
//                player.closeHandledScreen();
//                player.getServer().execute(() -> MenuManager.openSubmenu2(player));
//            }
//            case "back" -> {
//                player.closeHandledScreen();
//                player.getServer().execute(() -> MenuManager.openMainMenu(player));
//            }
//            // Add more actions here
//        }
    }
}