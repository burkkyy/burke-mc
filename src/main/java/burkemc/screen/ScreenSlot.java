package burkemc.screen;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

public class ScreenSlot extends Slot {
    private @Nullable
    final SlotDefinition definition;

    public ScreenSlot(Inventory inventory, int index, @Nullable SlotDefinition definition) {
        super(inventory, index, 8 + (index % 9) * 18, 18 + (index / 9) * 18);
        this.definition = definition;
    }

    public ScreenSlot(Inventory inventory, int index) {
        super(inventory, index, 8 + (index % 9) * 18, 18 + (index / 9) * 18);
        this.definition = null;
    }

    public void onClick(ServerPlayerEntity player) {
    }

    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return definition == null;
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        return definition == null;
    }

    @Override
    public ItemStack getStack() {
        if (definition == null) {
            return super.getStack();
        }

        ItemStack stack = super.getStack();
        return stack.isEmpty() ? make() : stack;
    }

    protected ItemStack make() {
        if (definition == null) {
            return super.getStack();
        }

        ItemStack stack = new ItemStack(definition.item());
        stack.set(DataComponentTypes.CUSTOM_NAME, definition.name());
        stack.set(DataComponentTypes.LORE, new LoreComponent(definition.lore()));
        return stack;
    }

    public boolean hasDefinition() {
        return definition != null;
    }
}
