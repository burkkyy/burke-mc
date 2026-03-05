package burkemc.screen;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

public class ScreenSlot extends Slot {
    private final SlotDefinition definition;

    public ScreenSlot(SlotDefinition definition, Inventory inventory, int index) {
        super(inventory, index, 8 + (index % 9) * 18, 18 + (index / 9) * 18);
        this.definition = definition;
    }

    public void onClick(ServerPlayerEntity player) {
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        return false;
    }

    @Override
    public ItemStack getStack() {
        ItemStack stack = super.getStack();
        return stack.isEmpty() ? make() : stack;
    }

    private ItemStack make() {
        ItemStack stack = new ItemStack(definition.item());
        stack.set(DataComponentTypes.CUSTOM_NAME, definition.name());
        stack.set(DataComponentTypes.LORE, new LoreComponent(definition.lore()));
        return stack;
    }
}
