package burkemc.screen;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class BaseMenuHandler extends GenericContainerScreenHandler {
    private static final int PLAYER_INV_START = 54;
    private static final int PLAYER_INV_END = 81;

    protected final ServerPlayerEntity player;
    private static final int MAX_SLOTS = 54; // 9x6
    private final Map<Integer, ScreenSlot> registeredSlots = new HashMap<>();

    protected BaseMenuHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
        this.player = player;
        initialize();
        build();
    }

    protected void initialize(){}

    protected void build() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            this.slots.set(i, registeredSlots.containsKey(i)
                    ? registeredSlots.get(i)
                    : new FillerSlot(getInventory(), i, x, y));
        }
    }

    protected void registerSlot(ScreenSlot slot) {
        int index = slot.getIndex();
        if (index < 0 || index >= MAX_SLOTS) {
            throw new IllegalArgumentException("Slot index " + index + " out of bounds (0-" + (MAX_SLOTS - 1) + ")");
        }
        if (registeredSlots.containsKey(index)) {
            throw new IllegalStateException("Slot " + index + " already registered in " + getClass().getSimpleName());
        }
        registeredSlots.put(index, slot);
    }

    protected ScreenSlot getScreenSlot(int index) {
        return registeredSlots.get(index); // null if not registered
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 54) {
            Slot slot = getSlot(slotIndex);
            if (slot instanceof ScreenSlot screenSlot) {
                screenSlot.onClick(this.player);
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    private static class FillerSlot extends Slot {
        public FillerSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
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
            return stack.isEmpty() ? makeItemStack() : stack;
        }

        private static ItemStack makeItemStack() {
            ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.empty());
            return pane;
        }
    }
}
