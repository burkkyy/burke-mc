package burkemc.item.builders;

import burkemc.item.ModItems;
import burkemc.util.ModRarity;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;

import static burkemc.BurkeMc.MOD_ID;

public class ModItemBuilder {

    private final String path;

    private Item.Settings settings = new Item.Settings();
    private Item vanillaItem = Items.STONE;

    private Text name = null;
    private boolean glint = false;
    private ModRarity rarity = ModRarity.COMMON;

    public ModItemBuilder(String path) {
        this.path = path;
    }

    public ModItemBuilder vanillaItem(Item item) {
        this.vanillaItem = item;
        return this;
    }

    public ModItemBuilder setCustomName(Text name) {
        this.name = name;
        return this;
    }

    public ModItemBuilder setCustomName(String name) {
        this.name = Text.literal(name);
        return this;
    }

    public ModItemBuilder rarity(ModRarity rarity){
        this.rarity = rarity;
        return this;
    }

    public ModItemBuilder glint() {
        this.glint = true;
        return this;
    }

    public ModItemBuilder settings(Item.Settings settings) {
        this.settings = settings;
        return this;
    }

    public ModItem build() {
        Identifier id = Identifier.of(MOD_ID, path);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        ModItem item = new ModItem(settings.registryKey(key), vanillaItem, name, rarity, glint);

        Registry.register(Registries.ITEM, id, item);
        ModItems.track(path, item);

        return item;
    }

    public static class ModItem extends Item implements PolymerItem {

        private final Item vanillaItem;
        private final Text name;
        private final ModRarity rarity;
        private final boolean glint;

        public ModItem(Settings settings, Item vanillaItem, Text name, ModRarity rarity, boolean glint) {
            super(settings);
            this.vanillaItem = vanillaItem;
            this.name = name;
            this.rarity = rarity;
            this.glint = glint;
        }

        @Override
        public Item getPolymerItem(ItemStack stack, PacketContext player) {
            return vanillaItem;
        }

        @Override
        public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
            return PolymerItem.super.getPolymerItemModel(stack, context);
        }

        @Override
        public ItemStack getPolymerItemStack(ItemStack stack, TooltipType tooltipType, PacketContext player) {
            ItemStack virtual = new ItemStack(vanillaItem, stack.getCount());

            if (name != null) {
                virtual.set(DataComponentTypes.CUSTOM_NAME,
                        name.copy().formatted(rarity.color).styled(style -> style.withItalic(false)));
            }

            List<Text> lore = new ArrayList<>();
            lore.add(rarity.text);
            virtual.set(DataComponentTypes.LORE, new LoreComponent(lore));

            if (glint) {
                virtual.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            }

            return virtual;
        }
    }
}
