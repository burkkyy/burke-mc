package burkemc.menu;

import burkemc.screen.BaseMenuHandler;
import burkemc.screen.ScreenSlot;
import burkemc.screen.SlotDefinition;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class MainMenu extends BaseMenuHandler {
    public static final int SETTINGS_SLOT_INDEX = 50;
    public static final int CLOSE_SLOT_INDEX = 49;
    public static final int COMING_SOON_SLOT_INDEX = 22;

    public MainMenu(int syncId, PlayerInventory playerInventory, Inventory inventory, ServerPlayerEntity player) {
        super(syncId, playerInventory, inventory, player);
    }

    @Override
    protected void initialize(){
        this.registerSlot(new ScreenSlot(
                getInventory(), CLOSE_SLOT_INDEX,
                SlotDefinition.of(Items.BARRIER, "Close", "Close this menu")
        ) {
            @Override
            public void onClick(ServerPlayerEntity player) {
                player.closeHandledScreen();
            }
        });

        this.registerSlot(new ScreenSlot(
                getInventory(), SETTINGS_SLOT_INDEX,
                SlotDefinition.of(Items.COMPARATOR, "Settings")
        ) {
            @Override
            public void onClick(ServerPlayerEntity player) {
                SettingsMenu.open(player);
            }
        });

        this.registerSlot(new ScreenSlot(
                getInventory(), COMING_SOON_SLOT_INDEX,
                SlotDefinition.of(Items.COMPASS, "Coming Soon...")
        ));
    }

    public static void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new MainMenu(syncId, inv, new SimpleInventory(54), player),
                Text.literal("Main Menu").styled(style -> style.withItalic(false))
        ));
    }
}
