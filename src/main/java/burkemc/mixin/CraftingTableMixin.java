package burkemc.mixin;

import burkemc.menu.CraftingMenu;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingTableBlock.class)
public class CraftingTableMixin {
    @Inject(at = @At("HEAD"), method = "onUse", cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                       BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient()) {
            return;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            CraftingMenu.open(serverPlayer, ScreenHandlerContext.create(world, pos));
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }
}