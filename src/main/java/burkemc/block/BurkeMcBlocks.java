package burkemc.block;

import burkemc.block.misc.BankTellerWorkstation;
import burkemc.block.misc.BankTellerWorkstationItem;
import burkemc.block.misc.ExampleBlock;
import burkemc.block.misc.ExampleBlockItem;
import burkemc.item.BurkeMcItems;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static burkemc.BurkeMc.MOD_ID;

public class BurkeMcBlocks {
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    // _TODO_ replace this with a builder pattern? YES ASAP
    public static final Block EXAMPLE_BLOCK = register("example_block", ExampleBlock::new, ExampleBlockItem::new);
    public static final Block BANK_TELLER_WORKSTATION = registerDirectional("bank_teller_workstation", BankTellerWorkstation::new, BankTellerWorkstationItem::new);

    private static <T extends Block> T register(String path, BiFunction<Block.Settings, BlockState, T> blockFactory, BiFunction<T, Item.Settings, BlockItem> itemFactory) {
        Identifier id = Identifier.of(MOD_ID, path);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);

        AbstractBlock.Settings settings = AbstractBlock.Settings.create().registryKey(blockKey);
        Identifier blockModelId = Identifier.of(MOD_ID, "block/" + path);
        BlockState polymerState =
                PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(blockModelId));

        T block = blockFactory.apply(settings, polymerState);
        Registry.register(Registries.BLOCK, blockKey, block);

        BlockItem item = itemFactory.apply(block, new Item.Settings().registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        BurkeMcItems.track(path, item);

        BLOCKS.put(path, block);
        return block;
    }

    private static <T extends Block> T registerDirectional(String path, BiFunction<Block.Settings, Map<Direction, BlockState>, T> blockFactory, BiFunction<T, Item.Settings, BlockItem> itemFactory) {
        Identifier id = Identifier.of(MOD_ID, path);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);

        Map<Direction, BlockState> polymerStates = new EnumMap<>(Direction.class);
        polymerStates.put(Direction.NORTH, PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(Identifier.of(MOD_ID, "block/" + path + "_north"))
        ));
        polymerStates.put(Direction.SOUTH, PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(Identifier.of(MOD_ID, "block/" + path + "_south"))
        ));
        polymerStates.put(Direction.EAST, PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(Identifier.of(MOD_ID, "block/" + path + "_east"))
        ));
        polymerStates.put(Direction.WEST, PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(Identifier.of(MOD_ID, "block/" + path + "_west"))
        ));

        T block = blockFactory.apply(AbstractBlock.Settings.create().registryKey(blockKey), polymerStates);
        Registry.register(Registries.BLOCK, blockKey, block);

        BlockItem item = itemFactory.apply(block, new Item.Settings().registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        BurkeMcItems.track(path, item);

        BLOCKS.put(path, block);
        return block;
    }


    public static void initialize() {  }
}
