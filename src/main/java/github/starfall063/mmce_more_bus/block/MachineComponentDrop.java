package github.starfall063.mmce_more_bus.block;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.starfall063.mmce_more_bus.tile.AbstractMarkerMEInputBus;
import github.starfall063.mmce_more_bus.tile.MEUniversalInventoryInputBus;
import github.starfall063.mmce_more_bus.tile.MEUniversalOutputBus;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Shared MMCE-style state transfer for machine-component drops. */
public final class MachineComponentDrop {
    public static final String CONFIGURED_KEY = "mmce_more_bus_configured";

    private MachineComponentDrop() {
    }

    public static ItemStack createDrop(Block block) {
        return new ItemStack(Item.getItemFromBlock(block));
    }

    public static void writeState(ItemStack drop, TileEntity tile, boolean configured) {
        if (!(tile instanceof MEMachineComponent) || !configured) return;
        NBTTagCompound state = new NBTTagCompound();
        ((MEMachineComponent) tile).writeCustomNBT(state);
        state.setBoolean(CONFIGURED_KEY, true);
        drop.setTagCompound(state);
    }

    public static void restoreState(World world, BlockPos pos, ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return;
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) return;
        if (!(tile instanceof MEMachineComponent)) return;
        ((MEMachineComponent) tile).readCustomNBT(stack.getTagCompound());
        tile.markDirty();
    }

    public static boolean isConfigured(TileEntity tile) {
        if (tile instanceof AbstractMarkerMEInputBus) {
            return ((AbstractMarkerMEInputBus<?>) tile).hasDropConfiguration();
        }
        if (tile instanceof MEUniversalInventoryInputBus) {
            return ((MEUniversalInventoryInputBus) tile).hasDropConfiguration();
        }
        return tile instanceof MEUniversalOutputBus
                && ((MEUniversalOutputBus) tile).getDistinctResourceCount() > 0;
    }

    @SideOnly(Side.CLIENT)
    public static void appendConfiguredTooltip(ItemStack stack, List<String> tooltip) {
        if (stack != null && stack.hasTagCompound() && stack.getTagCompound().getBoolean(CONFIGURED_KEY)) {
            tooltip.add(I18n.format("tooltip.mmce_more_bus.configured"));
        }
    }

    public static void spawnDrop(World world, BlockPos pos, Block block) {
        if (world.isRemote) return;
        TileEntity tile = world.getTileEntity(pos);
        ItemStack drop = createDrop(block);
        if (tile instanceof MEMachineComponent) writeState(drop, tile, isConfigured(tile));
        Block.spawnAsEntity(world, pos, drop);
    }

}
