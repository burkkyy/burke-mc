package burkemc.menu;

import burkemc.IPlayerData;
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

public class SettingsMenuHandler extends BaseMenuHandler {
    public static final int CLOSE_SLOT_INDEX = 49;
    public static final int MENU_ITEM_SLOT_INDEX = 10;
    public static final int MENU_ITEM_TOGGLE_SLOT_INDEX = 19;

    public SettingsMenuHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ServerPlayerEntity player) {
        super(syncId, playerInventory, inventory, player);
    }

    @Override
    protected void initialize() {
        this.registerSlot(new ScreenSlot(
                SlotDefinition.of(Items.BARRIER, "Close", "Close this menu"),
                getInventory(), CLOSE_SLOT_INDEX
        ) {
            @Override
            public void onClick(ServerPlayerEntity player) {
                player.closeHandledScreen();
            }
        });

        this.registerSlot(new ScreenSlot(
                SlotDefinition.of(Items.NETHER_STAR, "Show Menu Item"),
                getInventory(), MENU_ITEM_SLOT_INDEX
        ));

        var showMenuItem = IPlayerData.of(player).getBurkeMcSettings().showMenuItem;

        if (showMenuItem) {
            this.registerSlot(new ScreenSlot(
                    SlotDefinition.of(Items.GREEN_WOOL, "Toggle"),
                    getInventory(), MENU_ITEM_TOGGLE_SLOT_INDEX
            ){
                @Override
                public void onClick(ServerPlayerEntity player) {
                    var playerData = IPlayerData.of(player);
                    playerData.getBurkeMcSettings().showMenuItem = false;
                    playerData.save();
                    MainMenuManager.ensureMenuItem(player);
                    SettingsMenuHandler.open(player);
                }
            });
        } else {
            this.registerSlot(new ScreenSlot(
                    SlotDefinition.of(Items.RED_WOOL, "Toggle"),
                    getInventory(), MENU_ITEM_TOGGLE_SLOT_INDEX
            ){
                @Override
                public void onClick(ServerPlayerEntity player) {
                    var playerData = IPlayerData.of(player);
                    playerData.getBurkeMcSettings().showMenuItem = true;
                    playerData.save();
                    MainMenuManager.ensureMenuItem(player);
                    SettingsMenuHandler.open(player);
                }
            });
        }
    }

    public static void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new SettingsMenuHandler(syncId, inv, new SimpleInventory(54), player),
                Text.literal("Settings")
        ));
    }
}
