package burkemc.menu;

import burkemc.item.builders.ModItemBuilder;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoreQuickCraftsMenu extends BaseMenuHandler {
    public static final int[] QUICK_CRAFT_SLOT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    public static final int GO_BACK_SLOT = 49;

    private final ScreenHandlerContext context;

    private CraftingRecipe[] quickCraftCraftingRecipes = new CraftingRecipe[QUICK_CRAFT_SLOT.length];
    private ItemStack[] quickCraftItemStacks = new ItemStack[QUICK_CRAFT_SLOT.length];
    private boolean updatingQuickCraft = false;
    private int lastInventoryHash = -1;

    public MoreQuickCraftsMenu(int syncId, PlayerInventory playerInventory, SimpleInventory inventory, ServerPlayerEntity player, ScreenHandlerContext context) {
        super(syncId, playerInventory, inventory, player);
        this.context = context;
        inventory.addListener(this::onContentChanged);
        this.updateQuickCraftSlots();
    }

    @Override
    protected void initialize() {
        for (var i : QUICK_CRAFT_SLOT) {
            this.registerSlot(new QuickCraftSlot(getInventory(), i));
        }

        this.registerSlot(new ScreenSlot(
                getInventory(), GO_BACK_SLOT,
                SlotDefinition.of(Items.ARROW, "Go Back")
        ) {
            @Override
            public void onClick(ServerPlayerEntity player) {
                CraftingMenu.open(player, MoreQuickCraftsMenu.this.context);
            }
        });
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        this.updateQuickCraftSlots();
    }

    private ItemStack quickCraftFromPlayerInventory(CraftingRecipe recipe) {
        ItemStack[] result = {ItemStack.EMPTY};

        context.run((world, pos) -> {
            if (world.isClient()) {
                return;
            }

            ItemStack desired = getVanillaCraftingRecipeResult(recipe, world.getRegistryManager());

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
                        .limit(QUICK_CRAFT_SLOT.length)
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
            if (ItemStack.areItemsEqual(this.getStack(), defaultItemStack)) {
                return;
            }
            MoreQuickCraftsMenu.this.craftQuickCraft(this.getIndex());
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int index) {
            MoreQuickCraftsMenu.this.shiftCraftQuickCraft(this.getIndex());
            return ItemStack.EMPTY;
        }
    }

    public static void open(ServerPlayerEntity player, ScreenHandlerContext context) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new MoreQuickCraftsMenu(syncId, inv, new SimpleInventory(54), player, context),
                Text.literal("Available Quick Crafts").styled(style -> style.withItalic(false))
        ));
    }
}
