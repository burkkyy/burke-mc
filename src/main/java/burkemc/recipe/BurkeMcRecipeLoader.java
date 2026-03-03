package burkemc.recipe;

import burkemc.BurkeMc;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jspecify.annotations.NonNull;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BurkeMcRecipeLoader implements SimpleSynchronousResourceReloadListener {
    public static final String RECIPES_PACKAGE = "custom_recipes";

    public static final List<BurkeMcRecipe> RECIPES = new ArrayList<>();

    @Override
    public @NonNull Identifier getFabricId(){
        return Identifier.of(BurkeMc.MOD_ID, RECIPES_PACKAGE);
    }

    @Override
    public void reload(ResourceManager manager){
        RECIPES.clear();

        manager.findResources(RECIPES_PACKAGE, id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (var reader = new InputStreamReader(resource.getInputStream())){
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                // Ensure json has required
                if(!json.has("pattern")){
                    throw new IllegalArgumentException("Missing pattern");
                }

                if(!json.has("key")){
                    throw new IllegalArgumentException("Missing key");
                }

                // Get and Store result
                JsonObject resultJson = json.getAsJsonObject("result");
                Identifier itemId = Identifier.of(JsonHelper.getString(resultJson, "item"));
                int resultCount = JsonHelper.getInt(resultJson, "count", 1);
                ItemStack resultStack = new ItemStack(Registries.ITEM.get(itemId), resultCount);

                // Prepare ingredient array
                ItemStack[] ingredients = new ItemStack[9];
                for (int i = 0; i < 9; i++) ingredients[i] = ItemStack.EMPTY;

                JsonArray pattern = json.getAsJsonArray("pattern");

                if(pattern.size() != 3){
                    throw new IllegalArgumentException("Pattern must be 3 rows");
                }

                JsonObject key = json.getAsJsonObject("key");

                for (int row = 0; row < 3; row++){
                    String line = pattern.get(row).getAsString();
                    for(int col = 0; col < 3; col++){
                        char c = line.charAt(col);

                        if (c == ' ') continue;

                        if(!key.has(String.valueOf(c))){
                            throw new IllegalArgumentException("Key missing for character: " + c);
                        }

                        JsonObject ingredientJson = key.getAsJsonObject(String.valueOf(c));
                        Identifier ingredientId = Identifier.of(JsonHelper.getString(ingredientJson, "item"));

                        int ingredientCount = JsonHelper.getInt(ingredientJson, "count", 1);

                        ingredients[row * 3 + col] = new ItemStack(Registries.ITEM.get(ingredientId), ingredientCount);
                    }
                }

                RECIPES.add(new BurkeMcRecipe(ingredients, resultStack));
            } catch (Exception error){
                System.err.println("Failed to load custom recipe: " + id);
                error.printStackTrace();
            }
        });
    }
}
