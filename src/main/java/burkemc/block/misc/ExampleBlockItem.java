package burkemc.block.misc;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

import static burkemc.BurkeMc.MOD_ID;

public class ExampleBlockItem extends PolymerBlockItem {
    public ExampleBlockItem(Block block, Item.Settings settings) {
        super(block, settings);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack stack, TooltipType tooltipType, PacketContext context) {
        ItemStack virtual = super.getPolymerItemStack(stack, tooltipType, context);
        virtual.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Example Block").styled(s -> s.withItalic(false)));
        virtual.set(DataComponentTypes.ITEM_MODEL, Identifier.of(MOD_ID, "example_item"));
        return virtual;
    }
}
