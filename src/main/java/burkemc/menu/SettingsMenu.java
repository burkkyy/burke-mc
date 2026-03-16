package burkemc.menu;

import burkemc.menu.manager.MainMenuManager;
import burkemc.player.IPlayerData;
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

public class SettingsMenu extends BaseMenuHandler {
    public static final int CLOSE_SLOT_INDEX = 49;
    public static final int MENU_ITEM_SLOT_INDEX = 10;
    public static final int MENU_ITEM_TOGGLE_SLOT_INDEX = 19;

    public SettingsMenu(int syncId, PlayerInventory playerInventory, Inventory inventory, ServerPlayerEntity player) {
        super(syncId, playerInventory, inventory, player);
    }

    @Override
    protected void initialize() {
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
                getInventory(), MENU_ITEM_SLOT_INDEX,
                SlotDefinition.of(Items.NETHER_STAR, "Show Menu Item")
        ));

        var showMenuItem = IPlayerData.of(player).getSettings().showMenuItem;

        if (showMenuItem) {
            this.registerSlot(new ScreenSlot(
                    getInventory(), MENU_ITEM_TOGGLE_SLOT_INDEX,
                    SlotDefinition.of(Items.GREEN_WOOL, "Toggle")
            ){
                @Override
                public void onClick(ServerPlayerEntity player) {
                    var playerData = IPlayerData.of(player);
                    playerData.getSettings().showMenuItem = false;
                    playerData.save();
                    MainMenuManager.ensureMenuItem(player);
                    SettingsMenu.open(player);
                }
            });
        } else {
            this.registerSlot(new ScreenSlot(
                    getInventory(), MENU_ITEM_TOGGLE_SLOT_INDEX,
                    SlotDefinition.of(Items.RED_WOOL, "Toggle")
            ){
                @Override
                public void onClick(ServerPlayerEntity player) {
                    var playerData = IPlayerData.of(player);
                    playerData.getSettings().showMenuItem = true;
                    playerData.save();
                    MainMenuManager.ensureMenuItem(player);
                    SettingsMenu.open(player);
                }
            });
        }
    }

    public static void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new SettingsMenu(syncId, inv, new SimpleInventory(54), player),
                Text.literal("Settings")
        ));
    }
}
