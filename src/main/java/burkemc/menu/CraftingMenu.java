package burkemc.menu;

import burkemc.BurkeMc;
import burkemc.item.builders.ModItemBuilder;
import burkemc.recipe.ModRecipe;
import burkemc.recipe.ModRecipeManager;
import burkemc.screen.BaseMenuHandler;
import burkemc.screen.ScreenSlot;
import burkemc.screen.SlotDefinition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftingMenu extends BaseMenuHandler {
    public static final int[] CRAFTING_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int RESULT_SLOT = 23;
    public static final int[] QUICK_CRAFT_SLOT = {16, 25, 34};
    public static final int VIEW_MORE_QUICK_CRAFTS_SLOT = 26;
    public static final int[] BOTTOM_FILLER_ROW = {46, 47, 48, 50, 51, 52, 53};
    public static final int VANILLA_CRAFTING_TABLE_SLOT = 45;
    public static final int CLOSE_SLOT = 49;

    private final ScreenHandlerContext context;

    private ModRecipe activeModRecipe = null;
    private boolean updatingResult = false;

    private List<ModRecipe> quickCraftModRecipes = List.of();
    private CraftingRecipe[] quickCraftCraftingRecipes = new CraftingRecipe[3];
    private ItemStack[] quickCraftItemStacks = new ItemStack[3];

    private boolean updatingQuickCraft = false;
    private int lastInventoryHash = -1;

    public CraftingMenu(int syncId, PlayerInventory playerInventory, SimpleInventory inventory, ServerPlayerEntity player, ScreenHandlerContext context) {
        super(syncId, playerInventory, inventory, player);
        this.context = context;
        inventory.addListener(this::onContentChanged);
        this.updateQuickCraftSlots();
    }

    @Override
    protected void initialize() {
        this.allowInventoryInteraction = true;

        for (var i : CRAFTING_SLOTS) {
            this.registerSlot(new ScreenSlot(getInventory(), i));
        }

        this.registerSlot(new ResultScreenSlot(getInventory(), RESULT_SLOT));

        for (var i : QUICK_CRAFT_SLOT) {
            this.registerSlot(new QuickCraftSlot(getInventory(), i));
        }

        this.registerSlot(new ScreenSlot(getInventory(), VIEW_MORE_QUICK_CRAFTS_SLOT, SlotDefinition.of(Items.COMPASS, "View more quick craft options")) {
            @Override
            public void onClick(ServerPlayerEntity player) {
                MoreQuickCraftsMenu.open(player, CraftingMenu.this.context);
            }
        });

        for (var i : BOTTOM_FILLER_ROW) {
            this.registerSlot(new ScreenSlot(getInventory(), i, SlotDefinition.of(Items.RED_STAINED_GLASS_PANE, "")));
        }

        this.registerSlot(new ScreenSlot(
                getInventory(), VANILLA_CRAFTING_TABLE_SLOT,
                SlotDefinition.of(Items.CRAFTING_TABLE, "Open Vanilla", "Open the vanilla crafting table", "with recipe book")
        ) {
            @Override
            public void onClick(ServerPlayerEntity player) {
                context.run((world, pos) -> {
                    player.closeHandledScreen();
                    net.minecraft.block.BlockState state = world.getBlockState(pos);
                    player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
                });
            }
        });

        this.registerSlot(new ScreenSlot(
                getInventory(), CLOSE_SLOT,
                SlotDefinition.of(Items.BARRIER, "Close")
        ) {
            @Override
            public void onClick(ServerPlayerEntity player) {
                player.closeHandledScreen();
            }
        });
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        this.updateResult();
        this.updateQuickCraftSlots();
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        for (int slot : CRAFTING_SLOTS) {
            ItemStack stack = getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                player.getInventory().offerOrDrop(stack);
                getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // _TODO_ PLEASE Refactor this. It was completely vibe coded
        if (index >= PLAYER_INV_START) {
            Slot slot = getSlot(index);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) return ItemStack.EMPTY;

            // First pass — try to stack onto existing matching items
            for (int craftSlot : CRAFTING_SLOTS) {
                if (stack.isEmpty()) break;
                Slot target = getSlot(craftSlot);
                ItemStack targetStack = target.getStack();
                if (!targetStack.isEmpty()
                        && ItemStack.areItemsAndComponentsEqual(targetStack, stack)
                        && targetStack.getCount() < targetStack.getMaxCount()) {
                    int space = targetStack.getMaxCount() - targetStack.getCount();
                    int toMove = Math.min(space, stack.getCount());
                    targetStack.increment(toMove);
                    stack.decrement(toMove);
                }
            }

            // Second pass — fill empty slots with remainder
            for (int craftSlot : CRAFTING_SLOTS) {
                if (stack.isEmpty()) break;
                Slot target = getSlot(craftSlot);
                if (target.getStack().isEmpty() && target.canInsert(stack)) {
                    target.setStack(stack.copy());
                    slot.setStack(ItemStack.EMPTY);
                    return ItemStack.EMPTY;
                }
            }

            return ItemStack.EMPTY;
        }

        if (index >= 0 && index < this.slots.size()) {
            Slot slot = getSlot(index);

            if (slot instanceof FillerSlot) {
                return ItemStack.EMPTY;
            }

            if (slot instanceof ScreenSlot screenSlot) {
                if (screenSlot.hasDefinition()) {
                    return screenSlot.quickMove(player, index);
                }
            }
        }

        if (!allowInventoryInteraction) {
            return ItemStack.EMPTY;
        }

        return super.quickMove(player, index);
    }

    private void updateResult() {
        if (context == null || this.updatingResult) {
            return;
        }

        this.updatingResult = true;

        try {
            this.context.run((world, pos) -> {
                if (world.isClient()) {
                    return;
                }

                Inventory inventory = getInventory();

                CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, java.util.List.of(
                        inventory.getStack(CRAFTING_SLOTS[0]), inventory.getStack(CRAFTING_SLOTS[1]), inventory.getStack(CRAFTING_SLOTS[2]),
                        inventory.getStack(CRAFTING_SLOTS[3]), inventory.getStack(CRAFTING_SLOTS[4]), inventory.getStack(CRAFTING_SLOTS[5]),
                        inventory.getStack(CRAFTING_SLOTS[6]), inventory.getStack(CRAFTING_SLOTS[7]), inventory.getStack(CRAFTING_SLOTS[8])
                ));

                var modMatch = ModRecipeManager.getFirstMatch(input);
                if (modMatch.isPresent()) {
                    activeModRecipe = modMatch.get();
                    ItemStack result = activeModRecipe.result();
                    inventory.setStack(RESULT_SLOT, result);
                    return;
                }

                activeModRecipe = null;
                var vanillaMatch = world.getRecipeManager().getSynchronizedRecipes().getFirstMatch(RecipeType.CRAFTING, input, world);
                if (vanillaMatch.isPresent()) {
                    ItemStack result = vanillaMatch.get().value().craft(input, world.getRegistryManager());
                    inventory.setStack(RESULT_SLOT, result);
                    return;
                }

                inventory.setStack(RESULT_SLOT, ItemStack.EMPTY);
            });
        } finally {
            this.updatingResult = false;
        }
    }

    private void updateQuickCraftSlots() {
        int currentHash = computePlayerInventoryHash();
        if (currentHash == lastInventoryHash) return;
        lastInventoryHash = currentHash;

        if (updatingQuickCraft) {
            return;
        }

        updatingQuickCraft = true;

        try {
            context.run((world, pos) -> {
                if (world.isClient()) {
                    return;
                }

                PlayerInventory playerInventory = super.player.getInventory();

                this.quickCraftCraftingRecipes = world.getRecipeManager().getSynchronizedRecipes()
                        .getAllOfType(RecipeType.CRAFTING)
                        .stream()
                        .filter(entry -> entry.value() instanceof ShapedRecipe || entry.value() instanceof ShapelessRecipe)
                        .filter(entry -> ModRecipeManager.canPlayerInventoryCraftRecipe(playerInventory, entry.value()))
                        .limit(3)
                        .map(RecipeEntry::value)
                        .toArray(CraftingRecipe[]::new);

                for (int i = 0; i < QUICK_CRAFT_SLOT.length; i++) {
                    if (i < this.quickCraftCraftingRecipes.length) {
                        CraftingRecipe recipe = this.quickCraftCraftingRecipes[i];
                        ItemStack recipeItemStack = getVanillaCraftingRecipeResult(recipe, world.getRegistryManager());
                        this.quickCraftItemStacks[i] = recipeItemStack;
                        getInventory().setStack(QUICK_CRAFT_SLOT[i], recipeItemStack);
                    } else {
                        this.quickCraftItemStacks[i] = ItemStack.EMPTY;
                        getInventory().setStack(QUICK_CRAFT_SLOT[i], ItemStack.EMPTY);
                    }
                }
            });
        } finally {
            updatingQuickCraft = false;
        }
    }

    private int computePlayerInventoryHash() {
        PlayerInventory inventory = super.player.getInventory();

        int hash = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            hash = 31 * hash + stack.getItem().hashCode();
            hash = 31 * hash + stack.getCount();
        }
        return hash;
    }

    private ItemStack consumeCraftingSlotsFromResultRecipe() {
        Inventory inventory = this.getInventory();
        ItemStack result = inventory.getStack(RESULT_SLOT);

        // Step 1 - consume ingredients
        if (BurkeMc.isModItem(result)) {
            // Deal with mod recipe case
            var requiredIngredients = activeModRecipe.ingredients();

            for (int i = 0; i < 9; i++) {
                var requiredIngredient = requiredIngredients[i];
                var requiredIngredientCount = requiredIngredient.getCount();
                ItemStack ingredient = inventory.getStack(CRAFTING_SLOTS[i]);
                ingredient.decrement(requiredIngredientCount);
                inventory.setStack(CRAFTING_SLOTS[i], ingredient.isEmpty() ? ItemStack.EMPTY : ingredient);
            }
        } else {
            // Deal with vanilla recipe case
            for (int slot : CRAFTING_SLOTS) {
                ItemStack ingredient = inventory.getStack(slot);
                if (!ingredient.isEmpty()) {
                    ingredient.decrement(1);
                    inventory.setStack(slot, ingredient.isEmpty() ? ItemStack.EMPTY : ingredient);
                }
            }
        }

        return result;
    }

    private void craftResult() {
        ItemStack result = this.consumeCraftingSlotsFromResultRecipe();

        // Step 2 - Give the player their crafted item
        ItemStack cursor = this.getCursorStack();

        // Attempt to give player crafted items in hand
        if (cursor.isEmpty()) {
            this.setCursorStack(result);
        } else if (ItemStack.areItemsAndComponentsEqual(cursor, result)
                && cursor.getCount() < cursor.getMaxCount()) {
            cursor.increment(result.getCount());
        } else {
            // Fallback to player inventory or drop
            this.player.getInventory().offerOrDrop(result);
        }

        this.getInventory().setStack(RESULT_SLOT, ItemStack.EMPTY);
    }

    private void shiftCraftResult() {
        Inventory inventory = this.getInventory();
        ItemStack startingResult = inventory.getStack(RESULT_SLOT).copy();

        for (int i = 0; i < 64; i++) {
            ItemStack currentResult = inventory.getStack(RESULT_SLOT);

            if (currentResult.isEmpty() || !ItemStack.areEqual(startingResult, currentResult)) {
                break;
            }

            ItemStack result = this.consumeCraftingSlotsFromResultRecipe();
            inventory.setStack(RESULT_SLOT, ItemStack.EMPTY);
            this.player.getInventory().offerOrDrop(result);

            this.updateResult();
        }

    }

    private ItemStack quickCraftFromPlayerInventory(CraftingRecipe recipe) {
        ItemStack[] result = {ItemStack.EMPTY};

        context.run((world, pos) -> {
            if (world.isClient()) {
                return;
            }

            ItemStack desired = getVanillaCraftingRecipeResult(recipe);

            if (desired.isEmpty()) {
                System.out.println("Player trying to make an invalid recipe?\n\trecipe: " + recipe + " found ItemStack: " + desired);
                return;
            }

            PlayerInventory playerInventory = super.player.getInventory();
            boolean canCraft = ModRecipeManager.canPlayerInventoryCraftRecipe(playerInventory, recipe);

            if (!canCraft) {
                return;
            }

            List<Ingredient> ingredients = recipe.getIngredientPlacement().getIngredients();
            Map<Integer, Integer> toConsume = new HashMap<>();

            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) continue;

                boolean found = false;

                for (int j = 0; j < playerInventory.size(); j++) {
                    ItemStack invStack = playerInventory.getStack(j);
                    if (invStack.isEmpty()) continue;
                    if (invStack.getItem() instanceof ModItemBuilder.ModItem) continue;
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
                ItemStack invStack = playerInventory.getStack(entry.getKey());
                invStack.decrement(entry.getValue());
                playerInventory.setStack(entry.getKey(), invStack.isEmpty() ? ItemStack.EMPTY : invStack);
            }

            result[0] = desired;
        });

        return result[0];
    }

    private void craftQuickCraft(int slotIndex) {
        int index = getQuickCraftIndex(slotIndex);

        if (index == -1) {
            this.updateQuickCraftSlots();
            return;
        }

        CraftingRecipe desired = this.quickCraftCraftingRecipes[index];

        if (desired == null) {
            this.updateQuickCraftSlots();
            return;
        }

        ItemStack result = this.quickCraftFromPlayerInventory(desired);

        if (!result.isEmpty()) {
            this.player.getInventory().offerOrDrop(result);
        }

        this.updateQuickCraftSlots();
    }

    private void shiftCraftQuickCraft(int slotIndex) {
        int index = getQuickCraftIndex(slotIndex);

        if (index == -1 || index >= this.quickCraftCraftingRecipes.length) {
            this.updateQuickCraftSlots();
            return;
        }

        CraftingRecipe desired = this.quickCraftCraftingRecipes[index];

        if (desired == null) {
            this.updateQuickCraftSlots();
            return;
        }

        for (int i = 0; i < 64; i++) {
            ItemStack result = this.quickCraftFromPlayerInventory(desired);

            if (result.isEmpty()) {
                break;
            }

            this.player.getInventory().offerOrDrop(result);
        }

        this.updateQuickCraftSlots();
    }

    private int getQuickCraftIndex(int slotIndex) {
        for (int i = 0; i < QUICK_CRAFT_SLOT.length; i++) {
            if (QUICK_CRAFT_SLOT[i] == slotIndex) return i;
        }
        return -1;
    }

    private ItemStack getVanillaCraftingRecipeResult(CraftingRecipe recipe) {
        ItemStack[] result = {ItemStack.EMPTY};

        context.run((world, pos) -> {
            List<Ingredient> ingredients = recipe.getIngredientPlacement().getIngredients();
            List<ItemStack> inputStacks = new java.util.ArrayList<>();

            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    inputStacks.add(ItemStack.EMPTY);
                } else {
                    ItemStack match = ingredient.getMatchingItems()
                            .findFirst()
                            .map(entry -> new ItemStack(entry.value()))
                            .orElse(ItemStack.EMPTY);
                    inputStacks.add(match);
                }
            }

            while (inputStacks.size() < 9) inputStacks.add(ItemStack.EMPTY);

            CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, inputStacks);
            result[0] = recipe.craft(input, world.getRegistryManager());
        });

        return result[0];
    }

    private ItemStack getVanillaCraftingRecipeResult(CraftingRecipe recipe, RegistryWrapper.WrapperLookup registries) {
        List<Ingredient> ingredients = recipe.getIngredientPlacement().getIngredients();
        List<ItemStack> inputStacks = new java.util.ArrayList<>();

        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                inputStacks.add(ItemStack.EMPTY);
            } else {
                ItemStack match = ingredient.getMatchingItems()
                        .findFirst()
                        .map(entry -> new ItemStack(entry.value()))
                        .orElse(ItemStack.EMPTY);
                inputStacks.add(match);
            }
        }

        while (inputStacks.size() < 9) inputStacks.add(ItemStack.EMPTY);

        CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, inputStacks);
        return recipe.craft(input, registries);
    }


    private class ResultScreenSlot extends ScreenSlot {
        public ResultScreenSlot(Inventory inventory, int index) {
            super(inventory, index, SlotDefinition.of(
                    Items.BARRIER,
                    Text.literal("Recipe Required").styled(s -> s.withColor(0xFF5555).withItalic(false)),
                    "In the crafting grid to the left",
                    "add items for a valid recipe"
            ));
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
        public void onClick(ServerPlayerEntity player) {
            CraftingMenu.this.craftResult();
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int index) {
            CraftingMenu.this.shiftCraftResult();
            return ItemStack.EMPTY;
        }
    }

    private class QuickCraftSlot extends ScreenSlot {
        public QuickCraftSlot(Inventory inventory, int index) {
            super(inventory, index, SlotDefinition.of(Items.LIGHT_GRAY_STAINED_GLASS_PANE, ""));
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
        public void onClick(ServerPlayerEntity player) {
            ItemStack defaultItemStack = super.make();
            if(ItemStack.areItemsEqual(this.getStack(), defaultItemStack)){
                return;
            }
            CraftingMenu.this.craftQuickCraft(this.getIndex());
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int index) {
            CraftingMenu.this.shiftCraftQuickCraft(this.getIndex());
            return ItemStack.EMPTY;
        }
    }

    public static void open(ServerPlayerEntity player, ScreenHandlerContext context) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new CraftingMenu(syncId, inv, new SimpleInventory(54), player, context),
                Text.literal("Craft Item").styled(style -> style.withItalic(false))
        ));
    }
}
