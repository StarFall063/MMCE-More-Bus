package github.starfall063.mmce_more_bus.proxy;


import github.starfall063.mmce_more_bus.MMCEMoreBus;
import github.starfall063.mmce_more_bus.gui.MMCEGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class CommonProxy {
    public void construction() {

    }

    public void preInit() {
    }

    public void init() {
        NetworkRegistry.INSTANCE.registerGuiHandler(MMCEMoreBus.instance, new MMCEGuiHandler());
    }

    public void postInit() {

    }

    public Object createMEItemInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public Object createMEFluidInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public Object createMEGasInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public Object createMEUniversalInventoryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public Object createMEUniversalOutputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public Object createMEOreDictionaryInputBusGui(EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public void applyMEUniversalOutputViewport(BlockPos position, NBTTagCompound viewport) {
    }
}
