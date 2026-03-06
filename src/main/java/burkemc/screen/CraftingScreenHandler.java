package burkemc.screen;

import burkemc.menu.MainMenuManager;
import burkemc.recipe.BurkeMcRecipe;
import burkemc.recipe.BurkeMcRecipeManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// _TODO_ refactor this class, current its doing too much and looks ugly
public class CraftingScreenHandler extends GenericContainerScreenHandler {
    private final PlayerInventory playerInventory;
    private int lastInventoryHash = -1;

    private final SimpleInventory craftingInventory;
    private final ScreenHandlerContext context;
    private BurkeMcRecipe activeBurkeMcRecipe = null;

    private static final int[] CRAFTING_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int[] QUICK_CRAFT_SLOT = {16, 25, 34};
    private static final int[] BOTTOM_FILLER_ROW = {46, 47, 48, 50, 51, 52, 53};
    private static final int RECIPE_BOOK_SLOT = 45;
    private static final int GO_BACK_SLOT = 49;
    private static final int PLAYER_INV_START = 54;
    private static final int PLAYER_INV_END = 81;

    private static final CraftingRecipeInput DUMMY_INPUT = CraftingRecipeInput.create(3, 3,
            java.util.List.of(
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
            )
    );

    public CraftingScreenHandler(int syncId, PlayerInventory playerInv, ScreenHandlerContext context) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInv, new SimpleInventory(54), 6);
        this.context = context;
        this.playerInventory = playerInv;
        this.craftingInventory = (SimpleInventory) this.getInventory();

        this.craftingInventory.addListener(this::onContentChanged);
        this.slots.clear();

        for (int i = 0; i < 54; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;

            if (isCraftingSlot(i)) {
                this.addSlot(new Slot(craftingInventory, i, x, y));
            } else if (i == RESULT_SLOT) {
                this.addSlot(new ResultSlot(craftingInventory, i, x, y));
            } else if (isQuickCraftSlot(i)) {
                this.addSlot(new QuickCraftSlot(craftingInventory, i, x, y));
            } else if (i == RECIPE_BOOK_SLOT) {
                this.addSlot(new RecipeBookSlot(craftingInventory, i, x, y));
            } else if (i == GO_BACK_SLOT) {
                this.addSlot(new CancelSlot(craftingInventory, i, x, y));
            } else if (isBottomFillerRow(i)) {
                this.addSlot(new BottomRowFillerSlot(craftingInventory, i, x, y));
            } else {
                this.addSlot(new FillerSlot(craftingInventory, i, x, y));
            }
        }

        // Player inventory (slots 54-80)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 140 + row * 18));
            }
        }
        // Player hotbar (slots 81-89)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
        }

        updateQuickCraftSlots();
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        updateResult();
        updateQuickCraftSlots();
    }

    private boolean updatingResult = false;

    private void updateResult() {
        if (updatingResult || context == null) {
            return;
        }

        updatingResult = true;

        try {
            this.context.run((world, pos) -> {
                if (world.isClient()) {
                    return;
                }

                CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, java.util.List.of(
                        craftingInventory.getStack(CRAFTING_SLOTS[0]), craftingInventory.getStack(CRAFTING_SLOTS[1]), craftingInventory.getStack(CRAFTING_SLOTS[2]),
                        craftingInventory.getStack(CRAFTING_SLOTS[3]), craftingInventory.getStack(CRAFTING_SLOTS[4]), craftingInventory.getStack(CRAFTING_SLOTS[5]),
                        craftingInventory.getStack(CRAFTING_SLOTS[6]), craftingInventory.getStack(CRAFTING_SLOTS[7]), craftingInventory.getStack(CRAFTING_SLOTS[8])
                ));

                var burkeMcMatch = BurkeMcRecipeManager.getFirstMatch(input);

                if (burkeMcMatch.isPresent()) {
                    activeBurkeMcRecipe = burkeMcMatch.get();
                    var result = activeBurkeMcRecipe.result();
                    craftingInventory.setStack(RESULT_SLOT, result);
                    return;
                }

                activeBurkeMcRecipe = null;
                var vanillaMatch = world.getRecipeManager().getSynchronizedRecipes().getFirstMatch(RecipeType.CRAFTING, input, world);

                if (vanillaMatch.isPresent()) {
                    ItemStack result = vanillaMatch.get().value().craft(input, world.getRegistryManager());
                    craftingInventory.setStack(RESULT_SLOT, result);
                } else {
                    craftingInventory.setStack(RESULT_SLOT, ItemStack.EMPTY);
                }

            });
        } finally {
            updatingResult = false;
        }
    }

    private void updateQuickCraftSlots() {
        int currentHash = computeInventoryHash();
        if (currentHash == lastInventoryHash) return;
        lastInventoryHash = currentHash;

        context.run((world, pos) -> {
            if (world.isClient()) return;

            Map<Item, Integer> available = new HashMap<>();
            for (int i = 0; i < playerInventory.size(); i++) {
                ItemStack stack = playerInventory.getStack(i);
                if (stack.isEmpty() || MainMenuManager.isMenuItem(stack)) {
                    continue;
                }
                available.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }

            List<ItemStack> craftable = world.getRecipeManager()
                    .getSynchronizedRecipes().getAllOfType(RecipeType.CRAFTING)
                    .stream()
                    .filter(craftingRecipeRecipeEntry -> canCraftFromInventory(craftingRecipeRecipeEntry.value(), available))
                    .map(entry -> {
                        try {
                            return entry.value().craft(DUMMY_INPUT, world.getRegistryManager());
                        } catch (Exception e) {
                            return ItemStack.EMPTY;
                        }
                    })
                    .filter(stack -> !stack.isEmpty())
                    .limit(3)
                    .toList();

            for (int i = 0; i < QUICK_CRAFT_SLOT.length; i++) {
                if (i < craftable.size()) {
                    craftingInventory.setStack(QUICK_CRAFT_SLOT[i], craftable.get(i));
                } else {
                    craftingInventory.setStack(QUICK_CRAFT_SLOT[i], ItemStack.EMPTY);
                }
            }
        });
    }

    private boolean canCraftFromInventory(CraftingRecipe recipe, Map<Item, Integer> available) {
        var ingredients = recipe.getIngredientPlacement().getIngredients();

        // Skip recipes with no real ingredients
        if (ingredients.isEmpty() || ingredients.stream().allMatch(Ingredient::isEmpty)) {
            return false;
        }

        Map<Item, Integer> remaining = new HashMap<>(available);

        for (Ingredient ingredient : recipe.getIngredientPlacement().getIngredients()) {
            if (ingredient.isEmpty()) continue;

            boolean found = false;
            for (Map.Entry<Item, Integer> entry : remaining.entrySet()) {
                if (ingredient.test(new ItemStack(entry.getKey())) && entry.getValue() > 0) {
                    remaining.merge(entry.getKey(), -1, Integer::sum);
                    found = true;
                    break;
                }
            }

            if (!found) return false;
        }

        return true;
    }

    private int computeInventoryHash() {
        int hash = 0;
        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);
            hash = 31 * hash + stack.getItem().hashCode();
            hash = 31 * hash + stack.getCount();
        }
        return hash;
    }

    private void quickCraft(PlayerEntity player, ItemStack desired, int repeatTimes) {
        if (desired.isEmpty()) return;

        context.run((world, pos) -> {
            if (world.isClient()) return;

            var match = world.getRecipeManager()
                    .getSynchronizedRecipes()
                    .getAllOfType(RecipeType.CRAFTING)
                    .stream()
                    .filter(entry -> {
                        try {
                            return ItemStack.areItemsEqual(
                                    entry.value().craft(DUMMY_INPUT, world.getRegistryManager()), desired);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst();

            if (match.isEmpty()) return;


            for (int t = 0; t < repeatTimes; t++) {
                var ingredients = match.get().value().getIngredientPlacement().getIngredients();
                Map<Integer, Integer> toConsume = new HashMap<>();

                for (Ingredient ingredient : ingredients) {
                    if (ingredient.isEmpty()) continue;

                    boolean found = false;

                    for (int j = 0; j < playerInventory.size(); j++) {
                        ItemStack invStack = playerInventory.getStack(j);
                        if (invStack.isEmpty()) continue;
                        if (!ingredient.test(invStack)) continue;

                        int alreadyTaking = toConsume.getOrDefault(j, 0);
                        int available = invStack.getCount() - alreadyTaking;

                        if (available > 0) {
                            toConsume.merge(j, 1, Integer::sum);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        updateQuickCraftSlots();
                        return;
                    }
                }

                for (Map.Entry<Integer, Integer> entry : toConsume.entrySet()) {
                    playerInventory.getStack(entry.getKey()).decrement(entry.getValue());
                }

                var result = match.get().value().craft(DUMMY_INPUT, world.getRegistryManager());
                player.getInventory().offerOrDrop(result);
            }

            updateQuickCraftSlots();
        });
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex == RESULT_SLOT) {
            Slot resultSlot = this.slots.get(RESULT_SLOT);
            ItemStack currentResult = resultSlot.getStack();

            while (ItemStack.areEqual(currentResult, resultSlot.getStack())) {
                ItemStack result = resultSlot.getStack();

                if (result.isEmpty() || result.getItem() == Items.BARRIER) {
                    break;
                }

                ItemStack toInsert = result.copy();
                if (!this.insertItem(toInsert, PLAYER_INV_START, this.slots.size(), true)) {
                    break;
                }

                resultSlot.onTakeItem(player, result);
            }

            return ItemStack.EMPTY;
        }

        if (this.slots.get(slotIndex) instanceof FillerSlot ||
                this.slots.get(slotIndex) instanceof CancelSlot) {
            return ItemStack.EMPTY;
        }

        return super.quickMove(player, slotIndex);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < this.slots.size()) {
            if (this.slots.get(slotIndex) instanceof CancelSlot) {
                if (actionType == net.minecraft.screen.slot.SlotActionType.QUICK_MOVE) {
                    return;
                }
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.closeHandledScreen();
                }
                return;
            }

            if (this.slots.get(slotIndex) instanceof RecipeBookSlot) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    context.run((world, pos) -> {
                        serverPlayer.closeHandledScreen();
                        net.minecraft.block.BlockState state = world.getBlockState(pos);
                        serverPlayer.openHandledScreen(state.createScreenHandlerFactory(world, pos));
                    });
                }
                return;
            }

            if (this.slots.get(slotIndex) instanceof FillerSlot || this.slots.get(slotIndex) instanceof BottomRowFillerSlot) {
                return;
            }

            if (this.slots.get(slotIndex) instanceof QuickCraftSlot) {
                ItemStack result = craftingInventory.getStack(QUICK_CRAFT_SLOT[slotIndex == QUICK_CRAFT_SLOT[0] ? 0 : slotIndex == QUICK_CRAFT_SLOT[1] ? 1 : 2]);
                if (actionType == net.minecraft.screen.slot.SlotActionType.QUICK_MOVE) {
                    quickCraft(player, result, 64);
                } else {
                    quickCraft(player, result, 1);
                }
                return;
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        for (int slot : CRAFTING_SLOTS) {
            ItemStack stack = craftingInventory.getStack(slot);
            if (!stack.isEmpty()) {
                player.getInventory().offerOrDrop(stack);
                craftingInventory.setStack(slot, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isCraftingSlot(int i) {
        for (int slot : CRAFTING_SLOTS) if (slot == i) return true;
        return false;
    }

    private static boolean isQuickCraftSlot(int i) {
        for (int slot : QUICK_CRAFT_SLOT) if (slot == i) return true;
        return false;
    }

    private static boolean isBottomFillerRow(int i) {
        for (int slot : BOTTOM_FILLER_ROW) if (slot == i) return true;
        return false;
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

    private static class BottomRowFillerSlot extends Slot {
        public BottomRowFillerSlot(Inventory inventory, int index, int x, int y) {
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
            ItemStack pane = new ItemStack(Items.RED_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.empty());
            return pane;
        }
    }

    private class ResultSlot extends Slot {
        public ResultSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            if (activeBurkeMcRecipe != null) {
                var requiredIngredients = activeBurkeMcRecipe.ingredients();

                for (int i = 0; i < 9; i++) {
                    var requiredIngredient = requiredIngredients[i];
                    var requiredIngredientCount = requiredIngredient.getCount();
                    ItemStack ingredient = craftingInventory.getStack(CRAFTING_SLOTS[i]);
                    ingredient.decrement(requiredIngredientCount);
                    craftingInventory.setStack(CRAFTING_SLOTS[i], ingredient.isEmpty() ? ItemStack.EMPTY : ingredient);
                }
            } else {
                for (int slot : CRAFTING_SLOTS) {
                    ItemStack ingredient = craftingInventory.getStack(slot);
                    if (!ingredient.isEmpty()) {
                        ingredient.decrement(1);
                        craftingInventory.setStack(slot, ingredient.isEmpty() ? ItemStack.EMPTY : ingredient);
                    }
                }
            }

            craftingInventory.setStack(RESULT_SLOT, ItemStack.EMPTY);
            super.onTakeItem(player, stack);
        }

        @Override
        public ItemStack getStack() {
            ItemStack stack = super.getStack();
            if (stack.isEmpty()) {
                ItemStack warning = new ItemStack(Items.BARRIER);

                warning.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal("Recipe Required").styled(s -> s.withColor(0xFF5555).withItalic(false)));

                net.minecraft.component.type.LoreComponent lore = new net.minecraft.component.type.LoreComponent(java.util.List.of(
                        Text.literal("In the crafting grid to the left").styled(s -> s.withColor(0xAAAAAA).withItalic(false)),
                        Text.literal("add items for a valid recipe").styled(s -> s.withColor(0xAAAAAA).withItalic(false))
                ));
                warning.set(DataComponentTypes.LORE, lore);

                return warning;
            }
            return stack;
        }
    }

    // TODO: impl quick craft slots, ie display what you could craft from player inv
    private static class QuickCraftSlot extends Slot {
        public QuickCraftSlot(Inventory inventory, int index, int x, int y) {
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
            ItemStack pane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.empty());
            return pane;
        }
    }

    private static class RecipeBookSlot extends Slot {
        public RecipeBookSlot(Inventory inventory, int index, int x, int y) {
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
            ItemStack table = new ItemStack(Items.CRAFTING_TABLE);
            table.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Open Vanilla").styled(s -> s.withColor(0xFFFFFF).withItalic(false)));

            net.minecraft.component.type.LoreComponent lore = new net.minecraft.component.type.LoreComponent(java.util.List.of(
                    Text.literal("Open the vanilla crafting table").styled(s -> s.withColor(0xAAAAAA).withItalic(false)),
                    Text.literal("with recipe book").styled(s -> s.withColor(0xAAAAAA).withItalic(false))
            ));
            table.set(DataComponentTypes.LORE, lore);
            return table;
        }
    }

    private static class CancelSlot extends Slot {
        public CancelSlot(Inventory inventory, int index, int x, int y) {
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
            ItemStack arrow = new ItemStack(Items.ARROW);
            arrow.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Cancel").styled(s -> s.withItalic(false)));
            return arrow;
        }
    }
}