package burkemc.recipe;

import burkemc.menu.manager.MainMenuManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModRecipeManager {

    public static List<ModRecipe> getAllRecipes() {
        return ModRecipeLoader.RECIPES;
    }

    public static Optional<ModRecipe> getFirstMatch(CraftingRecipeInput grid) {
        for (ModRecipe recipe : ModRecipeLoader.RECIPES) {
            if (recipe.matches(grid)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static boolean canPlayerInventoryCraftRecipe(PlayerInventory inventory, ModRecipe recipe) {
        return true;
    }

    public static boolean canPlayerInventoryCraftRecipe(PlayerInventory inventory, CraftingRecipe recipe) {
        Map<Item, Integer> playerInventoryMap = buildAvailableMapAndFilterPlayerInventory(inventory);

        IngredientPlacement placement = recipe.getIngredientPlacement();
        List<Ingredient> ingredients = placement.getIngredients();

        if (ingredients.isEmpty() || ingredients.stream().allMatch(Ingredient::isEmpty)) {
            return false;
        }

        for (Ingredient ingredient : ingredients){
            if(ingredient.isEmpty()){
                continue;
            }

            boolean found = false;
            for (Map.Entry<Item, Integer> entry : playerInventoryMap.entrySet()) {
                if (entry.getValue() > 0 && ingredient.test(new ItemStack(entry.getKey()))) {
                    playerInventoryMap.merge(entry.getKey(), -1, Integer::sum);
                    found = true;
                    break;
                }
            }

            if (!found){
                return false;
            }
        }

        return true;
    }

    private static Map<Item, Integer> buildAvailableMapAndFilterPlayerInventory(PlayerInventory playerInventory) {
        Map<Item, Integer> available = new HashMap<>();
        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);
            if (stack.isEmpty() || MainMenuManager.isMenuItem(stack)) {
                continue;
            }
            available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        return available;
    }
}
