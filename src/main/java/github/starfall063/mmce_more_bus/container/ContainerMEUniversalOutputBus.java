package github.starfall063.mmce_more_bus.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import github.starfall063.mmce_more_bus.tile.MEUniversalOutputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.List;

public final class ContainerMEUniversalOutputBus extends AEBaseContainer {
    private static final int RESOURCE_SLOT_COUNT = 36;
    private static final int RESOURCE_COLUMNS = 9;
    private static final int RESOURCE_SLOT_X = 8;
    private static final int RESOURCE_SLOT_Y = 24;
    private static final int PLAYER_INVENTORY_X = 0;
    private static final int PLAYER_INVENTORY_Y = 113;
    private static final int HOTBAR_Y = PLAYER_INVENTORY_Y + 58;
    private final MEUniversalOutputBus bus;
    private final ViewportItemHandler viewportItems = new ViewportItemHandler();

    public ContainerMEUniversalOutputBus(InventoryPlayer inventoryPlayer, MEUniversalOutputBus bus) {
        super(inventoryPlayer, bus);
        this.bus = bus;
        for (int slot = 0; slot < RESOURCE_SLOT_COUNT; slot++) {
            addSlotToContainer(createReadOnlyViewportSlot(viewportItems, slot, resourceSlotX(slot), resourceSlotY(slot)));
        }
        bindPlayerInventory(inventoryPlayer, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
    }

    static SlotDisabled createReadOnlyViewportSlot(IItemHandlerModifiable handler, int slot, int x, int y) {
        return new SlotDisabled(handler, slot, x, y);
    }

    static int resourceSlotCount() {
        return RESOURCE_SLOT_COUNT;
    }

    static int resourceSlotX(int slot) {
        return RESOURCE_SLOT_X + slot % RESOURCE_COLUMNS * 18;
    }

    static int resourceSlotY(int slot) {
        return RESOURCE_SLOT_Y + slot / RESOURCE_COLUMNS * 18;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.world == bus.getWorld() && player.getDistanceSq(bus.getPos()) <= 64.0D
                && player.world.getTileEntity(bus.getPos()) == bus;
    }

    private final class ViewportItemHandler implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return RESOURCE_SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= RESOURCE_SLOT_COUNT) return ItemStack.EMPTY;
            List<MEUniversalOutputBus.DisplayResource> viewport = bus.getClientViewport();
            if (slot >= viewport.size()) return ItemStack.EMPTY;

            ItemStack item = viewport.get(slot).getItem();
            if (item.isEmpty()) return ItemStack.EMPTY;
            ItemStack display = item.copy();
            display.setCount(1);
            return display;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            // Display-only slots cannot change the output buffer.
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
