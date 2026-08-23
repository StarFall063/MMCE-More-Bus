package github.starfall063.mmce_more_bus.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Persistent ghost-item configuration for one ME inventory input bus.
 */
public final class MEItemInventoryBusState {
    private static final int SLOT_COUNT = 16;
    private static final int MIN_STACK_SIZE = 1;
    private static final String KEY_MARKERS = "sfc_me_markers";
    private static final String KEY_MIN_STACK_SIZE = "sfc_me_min_stack_size";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_STACK = "Stack";

    private final ItemStack[] markers = new ItemStack[SLOT_COUNT];
    private int minStackSize = MIN_STACK_SIZE;

    public MEItemInventoryBusState() {
        clearMarkers();
    }

    private static ItemStack copyIdentity(ItemStack stack) {
        ItemStack identity = stack.copy();
        identity.setCount(1);
        return identity;
    }

    private static boolean matchesIdentity(ItemStack first, ItemStack second) {
        return !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.areItemsEqual(first, second)
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Marker slot out of range: " + slot);
        }
    }

    public int size() {
        return SLOT_COUNT;
    }

    public boolean hasMarkers() {
        for (ItemStack marker : markers) {
            if (!marker.isEmpty()) return true;
        }
        return false;
    }

    public ItemStack getMarker(int slot) {
        checkSlot(slot);
        return markers[slot].copy();
    }

    public boolean setMarker(int slot, ItemStack stack) {
        checkSlot(slot);
        if (stack.isEmpty()) {
            clearMarker(slot);
            return true;
        }

        ItemStack identity = copyIdentity(stack);
        if (containsIdentityInOtherSlot(slot, identity)) {
            return false;
        }

        markers[slot] = identity;
        return true;
    }

    public void clearMarker(int slot) {
        checkSlot(slot);
        markers[slot] = ItemStack.EMPTY;
    }

    public int getMinStackSize() {
        return minStackSize;
    }

    public void setMinStackSize(int value) {
        minStackSize = Math.max(MIN_STACK_SIZE, value);
    }

    public void readNBT(NBTTagCompound tag) {
        clearMarkers();
        setMinStackSize(tag.getInteger(KEY_MIN_STACK_SIZE));

        NBTTagList storedMarkers = tag.getTagList(KEY_MARKERS, 10);
        List<NBTTagCompound> sortedMarkers = new ArrayList<>();
        for (int index = 0; index < storedMarkers.tagCount(); index++) {
            sortedMarkers.add(storedMarkers.getCompoundTagAt(index));
        }
        sortedMarkers.sort(Comparator.comparingInt(marker -> marker.getByte(KEY_SLOT) & 0xFF));

        for (NBTTagCompound marker : sortedMarkers) {
            int slot = marker.getByte(KEY_SLOT) & 0xFF;
            if (slot >= SLOT_COUNT || !marker.hasKey(KEY_STACK, 10)) continue;

            ItemStack stack = new ItemStack(marker.getCompoundTag(KEY_STACK));
            if (!stack.isEmpty()) {
                setMarker(slot, stack);
            }
        }
    }

    public void writeNBT(NBTTagCompound tag) {
        NBTTagList storedMarkers = new NBTTagList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack marker = markers[slot];
            if (marker.isEmpty()) continue;

            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte(KEY_SLOT, (byte) slot);
            entry.setTag(KEY_STACK, marker.writeToNBT(new NBTTagCompound()));
            storedMarkers.appendTag(entry);
        }
        tag.setTag(KEY_MARKERS, storedMarkers);
        tag.setInteger(KEY_MIN_STACK_SIZE, minStackSize);
    }

    private boolean containsIdentityInOtherSlot(int excludedSlot, ItemStack candidate) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (slot != excludedSlot && matchesIdentity(markers[slot], candidate)) {
                return true;
            }
        }
        return false;
    }

    private void clearMarkers() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            markers[slot] = ItemStack.EMPTY;
        }
    }
}
