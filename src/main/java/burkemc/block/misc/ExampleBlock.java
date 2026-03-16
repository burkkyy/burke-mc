package burkemc.block.misc;

import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import xyz.nucleoid.packettweaker.PacketContext;

public class ExampleBlock extends Block implements PolymerTexturedBlock {
    private final BlockState polymerBlockState;

    public ExampleBlock(Settings settings, BlockState polymerBlockState){
        super(settings);
        this.polymerBlockState = polymerBlockState;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return polymerBlockState;
    }
}
