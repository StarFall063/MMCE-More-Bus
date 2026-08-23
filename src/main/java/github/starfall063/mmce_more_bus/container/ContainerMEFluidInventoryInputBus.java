package github.starfall063.mmce_more_bus.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import github.starfall063.mmce_more_bus.tile.MEFluidInventoryInputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public final class ContainerMEFluidInventoryInputBus extends AEBaseContainer {
    private static final int SLOT_COLUMNS = 8;
    private static final int FIRST_SLOT_X = 17;
    private static final int FIRST_MARKER_Y = 20;
    private static final int FIRST_PREVIEW_Y = 38;
    private static final int PAIR_GROUP_HEIGHT = 52;
    private static final int PLAYER_INVENTORY_X = 0;
    private static final int PLAYER_INVENTORY_Y = 123;

    private final MEFluidInventoryInputBus bus;

    public ContainerMEFluidInventoryInputBus(InventoryPlayer inventoryPlayer, MEFluidInventoryInputBus bus) {
        super(inventoryPlayer, bus);
        this.bus = bus;
        IItemHandlerModifiable markerHandler = new MarkerItemHandler(bus);
        ItemStackHandler previewSlots = new ItemStackHandler(MEFluidInventoryInputBus.SLOT_COUNT);

        for (int slot = 0; slot < MEFluidInventoryInputBus.SLOT_COUNT; slot++) {
            addSlotToContainer(new SlotFake(markerHandler, slot, slotX(slot), markerY(slot)));
            addSlotToContainer(new SlotDisabled(previewSlots, slot, slotX(slot), previewY(slot)));
        }
        bindPlayerInventory(inventoryPlayer, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
    }

    public static int markerSlotCount() {
        return MEFluidInventoryInputBus.SLOT_COUNT;
    }

    public static int previewSlotCount() {
        return MEFluidInventoryInputBus.SLOT_COUNT;
    }

    static int slotX(int slot) {
        return FIRST_SLOT_X + slot % SLOT_COLUMNS * 18;
    }

    static int markerY(int slot) {
        return FIRST_MARKER_Y + slot / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    static int previewY(int slot) {
        return FIRST_PREVIEW_Y + slot / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.world == bus.getWorld()
                && player.getDistanceSq(bus.getPos()) <= 64.0D
                && player.world.getTileEntity(bus.getPos()) == bus;
    }

    private static final class MarkerItemHandler implements IItemHandlerModifiable {
        private final MEFluidInventoryInputBus bus;

        private MarkerItemHandler(MEFluidInventoryInputBus bus) {
            this.bus = bus;
        }

        @Override
        public int getSlots() {
            return MEFluidInventoryInputBus.SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            FluidStack fluid = FluidUtil.getFluidContained(stack);
            if (fluid == null) bus.clearMarker(slot);
            else bus.setMarker(slot, fluid);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!simulate) setStackInSlot(slot, stack);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!simulate && amount > 0) bus.clearMarker(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
