package github.starfall063.mmce_more_bus.gui;

import github.starfall063.mmce_more_bus.MMCEMoreBus;
import github.starfall063.mmce_more_bus.container.*;
import github.starfall063.mmce_more_bus.tile.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class MMCEGuiHandler implements IGuiHandler {
    public static final int ME_ITEM_INVENTORY_INPUT_BUS = 0;
    public static final int ME_FLUID_INVENTORY_INPUT_BUS = 1;
    public static final int ME_GAS_INVENTORY_INPUT_BUS = 2;
    public static final int ME_UNIVERSAL_INVENTORY_INPUT_BUS = 3;
    public static final int ME_UNIVERSAL_OUTPUT_BUS = 4;
    public static final int ME_ORE_DICTIONARY_INPUT_BUS = 5;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos position = new BlockPos(x, y, z);
        if (id == ME_ITEM_INVENTORY_INPUT_BUS) {
            if (!(world.getTileEntity(position) instanceof MEItemInventoryInputBus bus)) return null;
            return new ContainerMEItemInventoryInputBus(player.inventory, bus);
        }
        if (id == ME_FLUID_INVENTORY_INPUT_BUS) {
            if (!(world.getTileEntity(position) instanceof MEFluidInventoryInputBus bus)) return null;
            return new ContainerMEFluidInventoryInputBus(player.inventory, bus);
        }
        if (id == ME_GAS_INVENTORY_INPUT_BUS) {
            if (!(world.getTileEntity(position) instanceof MEGasInventoryInputBus bus)) return null;
            return new ContainerMEGasInventoryInputBus(player.inventory, bus);
        }
        if (id == ME_UNIVERSAL_INVENTORY_INPUT_BUS) {
            if (!(world.getTileEntity(position) instanceof MEUniversalInventoryInputBus bus)) return null;
            return new ContainerMEUniversalInventoryInputBus(player.inventory, bus);
        }
        if (id == ME_UNIVERSAL_OUTPUT_BUS) {
            if (!(world.getTileEntity(position) instanceof MEUniversalOutputBus bus)) return null;
            return new ContainerMEUniversalOutputBus(player.inventory, bus);
        }
        if (id == ME_ORE_DICTIONARY_INPUT_BUS) {
            if (!(world.getTileEntity(position) instanceof MEOreDictionaryInputBus bus)) return null;
            return new ContainerMEOreDictionaryInputBus(player.inventory, bus);
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ME_ITEM_INVENTORY_INPUT_BUS) {
            return MMCEMoreBus.proxy.createMEItemInventoryInputBusGui(player, world, x, y, z);
        }
        if (id == ME_FLUID_INVENTORY_INPUT_BUS) {
            return MMCEMoreBus.proxy.createMEFluidInventoryInputBusGui(player, world, x, y, z);
        }
        if (id == ME_GAS_INVENTORY_INPUT_BUS) {
            return MMCEMoreBus.proxy.createMEGasInventoryInputBusGui(player, world, x, y, z);
        }
        if (id == ME_UNIVERSAL_INVENTORY_INPUT_BUS) {
            return MMCEMoreBus.proxy.createMEUniversalInventoryInputBusGui(player, world, x, y, z);
        }
        if (id == ME_UNIVERSAL_OUTPUT_BUS) {
            return MMCEMoreBus.proxy.createMEUniversalOutputBusGui(player, world, x, y, z);
        }
        if (id == ME_ORE_DICTIONARY_INPUT_BUS) {
            return MMCEMoreBus.proxy.createMEOreDictionaryInputBusGui(player, world, x, y, z);
        }
        return null;
    }
}
