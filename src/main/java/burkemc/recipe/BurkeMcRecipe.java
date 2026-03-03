package burkemc.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.CraftingRecipeInput;

public record BurkeMcRecipe(ItemStack[] ingredients, ItemStack result) {

    public boolean matches(CraftingRecipeInput grid) {
        if (grid.getWidth() != 3 || grid.getHeight() != 3) {
            return false;
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int index = y * 3 + x;

                ItemStack required = ingredients[index] == null
                        ? ItemStack.EMPTY
                        : ingredients[index];

                ItemStack provided = grid.getStackInSlot(x, y);

                if (required.isEmpty() && provided.isEmpty()) continue;

                if (required.isEmpty() != provided.isEmpty()) return false;

                if (!ItemStack.areItemsEqual(required, provided)) return false;

                if (provided.getCount() < required.getCount()) return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }
}
