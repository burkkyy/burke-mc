package burkemc.item;

import burkemc.item.misc.EnchantedDiamond;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static burkemc.BurkeMc.MOD_ID;

public class BurkeMcItems {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    public static final Item ENCHANTED_DIAMOND = register("enchanted_diamond", EnchantedDiamond::new);

    private BurkeMcItems() {}

    private static <T extends Item> T register(String path, Function<Item.Settings, T> factory) {
        Identifier id = Identifier.of(MOD_ID, path);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        T item = factory.apply(new Item.Settings().registryKey(key));
        Registry.register(Registries.ITEM, key, item);

        ITEMS.put(path, item);
        return item;
    }

    public static void track(String path, Item item) {
        ITEMS.put(path, item);
    }

    public static void initialize() {}
}