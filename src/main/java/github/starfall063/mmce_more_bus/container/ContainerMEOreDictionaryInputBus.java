package github.starfall063.mmce_more_bus.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import github.starfall063.mmce_more_bus.tile.MEOreDictionaryInputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public final class ContainerMEOreDictionaryInputBus extends AEBaseContainer {
    private static final int SLOT_COLUMNS = 8;
    private static final int FIRST_SLOT_X = 17;
    private static final int FIRST_MARKER_Y = 20;
    private static final int FIRST_VIRTUAL_Y = 38;
    private static final int PAIR_GROUP_HEIGHT = 52;
    private static final int PLAYER_INVENTORY_X = 0;
    private static final int PLAYER_INVENTORY_Y = 123;
    private static final int HOTBAR_Y = 181;
    private static final int DISPLAY_SLOT_COUNT = MEOreDictionaryInputBus.SLOT_COUNT * 2;

    private final MEOreDictionaryInputBus bus;
    private final ItemStackHandler markerDisplay = new ItemStackHandler(MEOreDictionaryInputBus.SLOT_COUNT);
    private final ItemStackHandler virtualDisplay = new ItemStackHandler(MEOreDictionaryInputBus.SLOT_COUNT);

    public ContainerMEOreDictionaryInputBus(InventoryPlayer inventoryPlayer, MEOreDictionaryInputBus bus) {
        super(inventoryPlayer, bus);
        this.bus = bus;
        refreshDisplayCaches();
        for (int slot = 0; slot < MEOreDictionaryInputBus.SLOT_COUNT; slot++) {
            addSlotToContainer(new SlotDisabled(markerDisplay, slot, slotX(slot), markerY(slot)));
            addSlotToContainer(new SlotDisabled(virtualDisplay, slot, slotX(slot), virtualY(slot)));
        }
        bindPlayerInventory(inventoryPlayer, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
    }

    static boolean isDisplaySlot(int slotId) {
        return slotId >= 0 && slotId < DISPLAY_SLOT_COUNT;
    }

    private static ItemStack countOne(ItemStack stack) {
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        return copy;
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

    @Override
    public void detectAndSendChanges() {
        refreshDisplayCaches();
        super.detectAndSendChanges();
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (isDisplaySlot(slotId)) return ItemStack.EMPTY;
        return super.slotClick(slotId, dragType, clickType, player);
    }

    private void refreshDisplayCaches() {
        for (int slot = 0; slot < MEOreDictionaryInputBus.SLOT_COUNT; slot++) {
            markerDisplay.setStackInSlot(slot, countOne(bus.getMarker(slot)));
            virtualDisplay.setStackInSlot(slot, countOne(bus.getVirtualStack(slot)));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.world == bus.getWorld() && player.getDistanceSq(bus.getPos()) <= 64.0D
                && player.world.getTileEntity(bus.getPos()) == bus;
    }
}
