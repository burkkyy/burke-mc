package burkemc.item.misc;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class EnchantedDiamond extends Item implements PolymerItem {
    private final Item item = Items.DIAMOND;

    public EnchantedDiamond(Settings settings){
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext player) {
        return item;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return PolymerItem.super.getPolymerItemModel(stack, context);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack stack, TooltipType tooltipType, PacketContext player) {
        ItemStack virtual = new ItemStack(this.item, stack.getCount());

        virtual.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Enchanted Diamond").formatted(Formatting.GREEN).styled(style -> style.withItalic(false)));

        virtual.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        return virtual;
    }
}
