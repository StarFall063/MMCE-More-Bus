package github.starfall063.mmce_more_bus.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEGasBus;
import github.starfall063.mmce_more_bus.MMCEMoreBusCreativeTab;
import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.gui.MMCEGuiHandler;
import github.starfall063.mmce_more_bus.tile.MEGasInventoryInputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockMEGasInventoryInputBus extends BlockMEGasBus {
    public BlockMEGasInventoryInputBus() {
        setRegistryName(Tags.MOD_ID, "megasinventoryinputbus");
        setTranslationKey(Tags.MOD_ID + ".megasinventoryinputbus");
        setCreativeTab(MMCEMoreBusCreativeTab.INSTANCE);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MEGasInventoryInputBus();
    }

    @Override
    public boolean onBlockActivated(
            World world,
            BlockPos pos,
            IBlockState state,
            EntityPlayer player,
            EnumHand hand,
            EnumFacing facing,
            float hitX,
            float hitY,
            float hitZ
    ) {
        return MEInventoryBusActivation.openGui(
                world, pos, player, hand, MMCEGuiHandler.ME_GAS_INVENTORY_INPUT_BUS);
    }
}
