package github.starfall063.mmce_more_bus.registry;

import github.kasuminova.mmce.common.block.appeng.BlockMEMachineComponent;
import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.init.ModBlocks;
import github.starfall063.mmce_more_bus.tile.MEFluidInventoryInputBus;
import github.starfall063.mmce_more_bus.tile.MEGasInventoryInputBus;
import github.starfall063.mmce_more_bus.tile.MEItemInventoryInputBus;
import github.starfall063.mmce_more_bus.tile.MEOreDictionaryInputBus;
import github.starfall063.mmce_more_bus.tile.MEUniversalInventoryInputBus;
import github.starfall063.mmce_more_bus.tile.MEUniversalOutputBus;
import hellfirepvp.modularmachinery.common.block.BlockDynamicColor;
import hellfirepvp.modularmachinery.common.item.ItemBlockMEMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RegistryBlocks {
    private static final Map<Block, ItemBlock> ITEM_BLOCKS = new LinkedHashMap<>();
    private static final List<BlockDynamicColor> DYNAMIC_COLOR_BLOCKS = new ArrayList<>();

    private RegistryBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        DYNAMIC_COLOR_BLOCKS.clear();
        for (Block block : ModBlocks.ALL) {
            event.getRegistry().register(block);
            if (block instanceof BlockDynamicColor) {
                DYNAMIC_COLOR_BLOCKS.add((BlockDynamicColor) block);
            }
            registerTileEntity(block);
            ITEM_BLOCKS.put(block, createItemBlock(block));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerBlockColors(ColorHandlerEvent.Block event) {
        for (BlockDynamicColor dynamicColorBlock : DYNAMIC_COLOR_BLOCKS) {
            event.getBlockColors().registerBlockColorHandler(
                    dynamicColorBlock::getColorMultiplier,
                    (Block) dynamicColorBlock
            );
        }
    }

    private static ItemBlock createItemBlock(Block block) {
        ItemBlock itemBlock = block instanceof BlockMEMachineComponent
                ? new ItemBlockMEMachineComponent(block)
                : new ItemBlock(block);
        itemBlock.setRegistryName(block.getRegistryName());
        itemBlock.setTranslationKey(block.getTranslationKey());
        RegistryItems.trackDynamicColorItem(itemBlock);
        return itemBlock;
    }

    private static void registerTileEntity(Block block) {
        if (block == ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS) {
            GameRegistry.registerTileEntity(MEFluidInventoryInputBus.class, id("mefluidinventoryinputbus"));
        } else if (block == ModBlocks.ME_GAS_INVENTORY_INPUT_BUS) {
            GameRegistry.registerTileEntity(MEGasInventoryInputBus.class, id("megasinventoryinputbus"));
        } else if (block == ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS) {
            GameRegistry.registerTileEntity(MEItemInventoryInputBus.class, id("meiteminventoryinputbus"));
        } else if (block == ModBlocks.ME_ORE_DICTIONARY_INPUT_BUS) {
            GameRegistry.registerTileEntity(MEOreDictionaryInputBus.class, id("meoredictionaryinputbus"));
        } else if (block == ModBlocks.ME_UNIVERSAL_INVENTORY_INPUT_BUS) {
            GameRegistry.registerTileEntity(MEUniversalInventoryInputBus.class, id("meuniversalinputbus"));
        } else if (block == ModBlocks.ME_UNIVERSAL_OUTPUT_BUS) {
            GameRegistry.registerTileEntity(MEUniversalOutputBus.class, id("meuniversaloutputbus"));
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Tags.MOD_ID, path);
    }

    @Mod.EventBusSubscriber(modid = Tags.MOD_ID)
    public static final class Items {
        private Items() {
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            for (ItemBlock itemBlock : ITEM_BLOCKS.values()) {
                event.getRegistry().register(itemBlock);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
    public static final class Models {
        private Models() {
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            for (ItemBlock itemBlock : ITEM_BLOCKS.values()) {
                Item item = Item.getItemFromBlock(itemBlock.getBlock());
                ResourceLocation registryName = item.getRegistryName();
                ModelBakery.registerItemVariants(item, registryName);
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(registryName, "inventory"));
            }
        }
    }
}
