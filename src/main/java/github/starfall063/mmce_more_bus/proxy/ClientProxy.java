package github.starfall063.mmce_more_bus.proxy;

import github.starfall063.mmce_more_bus.gui.*;
import github.starfall063.mmce_more_bus.tile.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClientProxy extends CommonProxy {
    @Override
    public Object createMEItemInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        if (!(world.getTileEntity(new BlockPos(x, y, z)) instanceof MEItemInventoryInputBus bus)) return null;
        return new GuiMEItemInventoryInputBus(player.inventory, bus);
    }

    @Override
    public Object createMEFluidInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        if (!(world.getTileEntity(new BlockPos(x, y, z)) instanceof MEFluidInventoryInputBus bus)) return null;
        return new GuiMEFluidInventoryInputBus(player.inventory, bus);
    }

    @Override
    public Object createMEGasInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        if (!(world.getTileEntity(new BlockPos(x, y, z)) instanceof MEGasInventoryInputBus bus)) return null;
        return new GuiMEGasInventoryInputBus(player.inventory, bus);
    }

    @Override
    public Object createMEUniversalInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        if (!(world.getTileEntity(new BlockPos(x, y, z)) instanceof MEUniversalInventoryInputBus bus)) return null;
        return new GuiMEUniversalInventoryInputBus(player.inventory, bus);
    }

    @Override
    public Object createMEUniversalOutputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        if (!(world.getTileEntity(new BlockPos(x, y, z)) instanceof MEUniversalOutputBus bus)) return null;
        return new GuiMEUniversalOutputBus(player.inventory, bus);
    }

    @Override
    public Object createMEOreDictionaryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        if (!(world.getTileEntity(new BlockPos(x, y, z)) instanceof MEOreDictionaryInputBus bus)) return null;
        return new GuiMEOreDictionaryInputBus(player.inventory, bus);
    }

    @Override
    public void applyMEUniversalOutputViewport(BlockPos position, NBTTagCompound viewport) {
        World world = Minecraft.getMinecraft().world;
        if (world == null) return;
        if (world.getTileEntity(position) instanceof MEUniversalOutputBus bus) {
            bus.readViewportState(viewport);
        }
    }

    @Override
    public void preInit() {
        super.preInit();
    }
}
