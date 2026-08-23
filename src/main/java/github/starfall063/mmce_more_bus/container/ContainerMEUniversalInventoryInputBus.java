package github.starfall063.mmce_more_bus.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import github.starfall063.mmce_more_bus.tile.MEUniversalInventoryInputBus;
import mekanism.api.gas.IGasItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public final class ContainerMEUniversalInventoryInputBus extends AEBaseContainer {
    private static final int SLOT_COLUMNS = 8;
    private static final int FIRST_SLOT_X = 17;
    private static final int FIRST_MARKER_Y = 20;
    private static final int FIRST_PREVIEW_Y = 38;
    private static final int PAIR_GROUP_HEIGHT = 52;
    private static final int PLAYER_INVENTORY_X = 0;
    private static final int PLAYER_INVENTORY_Y = 123;

    private final MEUniversalInventoryInputBus bus;
    private final MarkerItemHandler markerHandler;

    public ContainerMEUniversalInventoryInputBus(InventoryPlayer inventoryPlayer, MEUniversalInventoryInputBus bus) {
        super(inventoryPlayer, bus);
        this.bus = bus;
        markerHandler = new MarkerItemHandler();
        IItemHandlerModifiable markers = markerHandler;
        ItemStackHandler previews = new ItemStackHandler(MEUniversalInventoryInputBus.SLOT_COUNT);
        for (int slot = 0; slot < MEUniversalInventoryInputBus.SLOT_COUNT; slot++) {
            addSlotToContainer(new SlotFake(markers, slot, slotX(slot), markerY(slot)));
            addSlotToContainer(new SlotDisabled(previews, slot, slotX(slot), previewY(slot)));
        }
        bindPlayerInventory(inventoryPlayer, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
    }

    public static MEUniversalInventoryInputBus.MarkerType markerTypeFor(ItemStack stack) {
        if (stack.isEmpty()) return MEUniversalInventoryInputBus.MarkerType.EMPTY;
        try {
            if (FluidUtil.getFluidContained(stack) != null) return MEUniversalInventoryInputBus.MarkerType.FLUID;
        } catch (RuntimeException ignored) {
            // Capability registration can be incomplete during early client/test startup.
        }
        if (stack.getItem() instanceof IGasItem gasItem && gasItem.getGas(stack) != null) {
            return MEUniversalInventoryInputBus.MarkerType.GAS;
        }
        return MEUniversalInventoryInputBus.MarkerType.ITEM;
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

    private final class MarkerItemHandler implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return MEUniversalInventoryInputBus.SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return bus.getItemMarker(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            MEUniversalInventoryInputBus.MarkerType type = markerTypeFor(stack);
            if (type == MEUniversalInventoryInputBus.MarkerType.EMPTY) {
                bus.clearMarker(slot);
            } else if (type == MEUniversalInventoryInputBus.MarkerType.FLUID) {
                bus.setFluidMarker(slot, FluidUtil.getFluidContained(stack));
            } else if (type == MEUniversalInventoryInputBus.MarkerType.GAS) {
                IGasItem gasItem = (IGasItem) stack.getItem();
                bus.setGasMarker(slot, gasItem.getGas(stack));
            } else {
                bus.setItemMarker(slot, stack);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!simulate) setStackInSlot(slot, stack);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (simulate || amount <= 0) return ItemStack.EMPTY;
            ItemStack existing = getStackInSlot(slot);
            if (!existing.isEmpty()) bus.clearMarker(slot);
            return existing;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
