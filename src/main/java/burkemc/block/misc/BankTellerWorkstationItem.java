package burkemc.block.misc;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

import static burkemc.BurkeMc.MOD_ID;

public class BankTellerWorkstationItem  extends PolymerBlockItem {
    public BankTellerWorkstationItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack stack, TooltipType tooltipType, PacketContext context) {
        ItemStack virtual = super.getPolymerItemStack(stack, tooltipType, context);
        virtual.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Bank Teller Workstation").styled(s -> s.withItalic(false)));
        virtual.set(DataComponentTypes.ITEM_MODEL, Identifier.of(MOD_ID, "bank_teller_workstation"));
        return virtual;
    }
}
