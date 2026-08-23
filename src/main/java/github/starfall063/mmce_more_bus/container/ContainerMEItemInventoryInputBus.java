package github.starfall063.mmce_more_bus.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import github.starfall063.mmce_more_bus.tile.MEItemInventoryInputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public final class ContainerMEItemInventoryInputBus extends AEBaseContainer {
    private static final int SLOT_COLUMNS = 8;
    private static final int FIRST_SLOT_X = 17;
    private static final int FIRST_MARKER_Y = 20;
    private static final int FIRST_VIRTUAL_Y = 38;
    private static final int PAIR_GROUP_HEIGHT = 52;
    private static final int PLAYER_INVENTORY_X = 0;
    private static final int PLAYER_INVENTORY_Y = 123;
    private static final int HOTBAR_Y = 181;

    private final MEItemInventoryInputBus bus;
    private final IItemHandlerModifiable markerHandler;
    private final ItemStackHandler virtualDisplay = new DisplayItemHandler("virtual");

    public ContainerMEItemInventoryInputBus(InventoryPlayer inventoryPlayer, MEItemInventoryInputBus bus) {
        super(inventoryPlayer, bus);
        this.bus = bus;
        this.markerHandler = new MarkerItemHandler(bus);
        refreshDisplayCaches();

        for (int slot = 0; slot < MEItemInventoryInputBus.SLOT_COUNT; slot++) {
            int column = slot % SLOT_COLUMNS;

            addSlotToContainer(createMarkerSlot(markerHandler, slot, slotX(slot), markerY(slot)));
            addSlotToContainer(createVirtualSlot(virtualDisplay, slot, slotX(slot), virtualY(slot)));
        }
        addPlayerInventory(inventoryPlayer);
    }

    private static void setDisplayStack(ItemStackHandler display, int slot, ItemStack replacement) {
        if (!ItemStack.areItemStacksEqual(display.getStackInSlot(slot), replacement)) {
            display.setStackInSlot(slot, replacement);
        }
    }

    static int slotX(int slot) {
        return FIRST_SLOT_X + slot % SLOT_COLUMNS * 18;
    }

    static int markerY(int slot) {
        return FIRST_MARKER_Y + slot / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    static int virtualY(int slot) {
        return FIRST_VIRTUAL_Y + slot / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    static int playerInventoryY(int row) {
        return PLAYER_INVENTORY_Y + row * 18;
    }

    static int playerInventoryX() {
        return PLAYER_INVENTORY_X;
    }

    static int hotbarY() {
        return HOTBAR_Y;
    }

    static SlotFake createMarkerSlot(IItemHandlerModifiable handler, int slot, int x, int y) {
        return new SlotFake(handler, slot, x, y);
    }

    static SlotDisabled createVirtualSlot(ItemStackHandler handler, int slot, int x, int y) {
        return new SlotDisabled(handler, slot, x, y);
    }

    private void addPlayerInventory(InventoryPlayer inventoryPlayer) {
        bindPlayerInventory(inventoryPlayer, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
    }

    @Override
    public void detectAndSendChanges() {
        refreshDisplayCaches();
        super.detectAndSendChanges();
    }

    private void refreshDisplayCaches() {
        for (int slot = 0; slot < MEItemInventoryInputBus.SLOT_COUNT; slot++) {
            setDisplayStack(virtualDisplay, slot, bus.getVirtualStack(slot));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.world == bus.getWorld() && player.getDistanceSq(bus.getPos()) <= 64.0D && player.world.getTileEntity(bus.getPos()) == bus;
    }

    private static final class DisplayItemHandler extends ItemStackHandler {
        private DisplayItemHandler(String type) {
            super(MEItemInventoryInputBus.SLOT_COUNT);
        }
    }

    private static final class MarkerItemHandler implements IItemHandlerModifiable {
        private final MEItemInventoryInputBus bus;

        private MarkerItemHandler(MEItemInventoryInputBus bus) {
            this.bus = bus;
        }

        @Override
        public int getSlots() {
            return MEItemInventoryInputBus.SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return bus.getMarker(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (stack.isEmpty()) bus.clearMarker(slot);
            else bus.setMarker(slot, stack);
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
