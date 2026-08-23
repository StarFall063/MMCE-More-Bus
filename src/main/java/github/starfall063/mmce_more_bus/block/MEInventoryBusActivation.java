package github.starfall063.mmce_more_bus.block;

import github.starfall063.mmce_more_bus.MMCEMoreBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

final class MEInventoryBusActivation {
    private MEInventoryBusActivation() {
    }

    static boolean openGui(World world, BlockPos pos, EntityPlayer player, EnumHand hand, int guiId) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (!world.isRemote) {
            player.openGui(
                    MMCEMoreBus.instance,
                    guiId,
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
        }
        return true;
    }
}
