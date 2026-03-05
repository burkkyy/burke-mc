package burkemc.menu;

import burkemc.IPlayerData;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

public class MainMenuManager {
    public static final Item MENU_ITEM = Items.NETHER_STAR;
    public static final int MENU_ITEM_SLOT = 8;

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> ensureMenuItem(newPlayer));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ensureMenuItem(handler.player));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient() && isMenuItem(stack)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (isMenuItem(stack)) {
                if (!world.isClient()) {
                    MainMenuHandler.open((ServerPlayerEntity) player);
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });

        // Left-click on a block
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient() && isMenuItem(stack)) {
                MainMenuHandler.open((ServerPlayerEntity) player);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Left-click on an entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient() && isMenuItem(stack)) {
                MainMenuHandler.open((ServerPlayerEntity) player);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    private static ItemStack makeMenuButton() {
        ItemStack menuStack = new ItemStack(MENU_ITEM);

        // _TODO_: find better restricted items naming
        var nbt = new NbtCompound();
        nbt.putBoolean("isMenuItem", true);
        menuStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        MutableText title = Text.literal("Menu ")
                .styled(style -> style.withItalic(false).withColor(Formatting.WHITE));
        MutableText subText = Text.literal("(Click to Open)")
                .styled(style -> style.withColor(Formatting.GRAY).withItalic(false));
        menuStack.set(DataComponentTypes.CUSTOM_NAME, title.append(subText));
        return menuStack;
    }

    public static boolean isMenuItem(ItemStack stack) {
        if (!stack.isOf(MENU_ITEM)) {
            return false;
        }

        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);

        if (data == null) {
            return false;
        }

        return data.copyNbt().getBoolean("isMenuItem", false);
    }

    public static void ensureMenuItem(ServerPlayerEntity player) {
        var settings = IPlayerData.of(player).getBurkeMcSettings();

        var inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            if (i != MENU_ITEM_SLOT && isMenuItem(inv.getStack(i))) {
                inv.setStack(i, ItemStack.EMPTY);
            }
        }

        ItemStack current = inv.getStack(MENU_ITEM_SLOT);
        if(settings.showMenuItem){
            if (!isMenuItem(current) || current.getCount() != 1) {
                ItemStack menuStack = makeMenuButton();
                inv.setStack(MENU_ITEM_SLOT, menuStack);
            }
        } else {
            if(isMenuItem(current)){
                inv.setStack(MENU_ITEM_SLOT, ItemStack.EMPTY);
            }
        }
    }
}
