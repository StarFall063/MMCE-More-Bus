package github.starfall063.mmce_more_bus.init;

import github.starfall063.mmce_more_bus.block.*;
import net.minecraft.block.Block;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ModBlocks {
    public static final String ME_FLUID_INVENTORY_INPUT_BUS_ID = "mefluidinventoryinputbus";
    public static final String ME_GAS_INVENTORY_INPUT_BUS_ID = "megasinventoryinputbus";
    public static final String ME_ITEM_INVENTORY_INPUT_BUS_ID = "meiteminventoryinputbus";
    public static final String ME_ORE_DICTIONARY_INPUT_BUS_ID = "meoredictionaryinputbus";
    public static final String ME_UNIVERSAL_INVENTORY_INPUT_BUS_ID = "meuniversalinputbus";
    public static final String ME_UNIVERSAL_OUTPUT_BUS_ID = "meuniversaloutputbus";

    public static final BlockMEFluidInventoryInputBus ME_FLUID_INVENTORY_INPUT_BUS = new BlockMEFluidInventoryInputBus();
    public static final BlockMEGasInventoryInputBus ME_GAS_INVENTORY_INPUT_BUS = new BlockMEGasInventoryInputBus();
    public static final BlockMEItemInventoryInputBus ME_ITEM_INVENTORY_INPUT_BUS = new BlockMEItemInventoryInputBus();
    public static final BlockMEOreDictionaryInputBus ME_ORE_DICTIONARY_INPUT_BUS = new BlockMEOreDictionaryInputBus();
    public static final BlockMEUniversalInventoryInputBus ME_UNIVERSAL_INVENTORY_INPUT_BUS = new BlockMEUniversalInventoryInputBus();
    public static final BlockMEUniversalOutputBus ME_UNIVERSAL_OUTPUT_BUS = new BlockMEUniversalOutputBus();

    public static final List<Block> ALL = Collections.unmodifiableList(Arrays.asList(
            ME_FLUID_INVENTORY_INPUT_BUS,
            ME_GAS_INVENTORY_INPUT_BUS,
            ME_ITEM_INVENTORY_INPUT_BUS,
            ME_ORE_DICTIONARY_INPUT_BUS,
            ME_UNIVERSAL_INVENTORY_INPUT_BUS,
            ME_UNIVERSAL_OUTPUT_BUS
    ));

    private ModBlocks() {
    }

    public static void init() {
        // Class loading creates the singleton block instances before Forge registry events.
    }
}
