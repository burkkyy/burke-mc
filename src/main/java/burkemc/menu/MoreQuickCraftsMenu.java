package burkemc.menu;

import burkemc.screen.BaseMenuHandler;
import burkemc.screen.ScreenSlot;
import burkemc.screen.SlotDefinition;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class MoreQuickCraftsMenu extends BaseMenuHandler {
    public static final int GO_BACK_SLOT = 49;

    private final ScreenHandlerContext context;

    public MoreQuickCraftsMenu(int syncId, PlayerInventory playerInventory, SimpleInventory inventory, ServerPlayerEntity player, ScreenHandlerContext context) {
        super(syncId, playerInventory, inventory, player);
        this.context = context;
    }

    @Override
    protected void initialize() {
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

    public static void open(ServerPlayerEntity player, ScreenHandlerContext context) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new MoreQuickCraftsMenu(syncId, inv, new SimpleInventory(54), player, context),
                Text.literal("Available Quick Crafts").styled(style -> style.withItalic(false))
        ));
    }
}
