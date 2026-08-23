package github.starfall063.mmce_more_bus.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEItemBus;
import github.starfall063.mmce_more_bus.MMCEMoreBus;
import github.starfall063.mmce_more_bus.MMCEMoreBusCreativeTab;
import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.gui.MMCEGuiHandler;
import github.starfall063.mmce_more_bus.tile.MEItemInventoryInputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockMEItemInventoryInputBus extends BlockMEItemBus {
    public BlockMEItemInventoryInputBus() {
        setRegistryName(Tags.MOD_ID, "meiteminventoryinputbus");
        setTranslationKey(Tags.MOD_ID + ".meiteminventoryinputbus");
        setCreativeTab(MMCEMoreBusCreativeTab.INSTANCE);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MEItemInventoryInputBus();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (!world.isRemote) {
            player.openGui(
                    MMCEMoreBus.instance,
                    MMCEGuiHandler.ME_ITEM_INVENTORY_INPUT_BUS,
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
        }
        return true;
    }
}
