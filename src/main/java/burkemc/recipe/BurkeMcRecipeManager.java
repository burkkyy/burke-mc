package burkemc.recipe;

import net.minecraft.recipe.input.CraftingRecipeInput;

import java.util.Optional;

public class BurkeMcRecipeManager {
    public static Optional<BurkeMcRecipe> getFirstMatch(CraftingRecipeInput grid){
        for (BurkeMcRecipe recipe : BurkeMcRecipeLoader.RECIPES) {
            if (recipe.matches(grid)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }
}
