package github.starfall063.mmce_more_bus.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Immutable ME availability snapshot used by MMCE's asynchronous recipe check.
 */
public final class MEItemInventorySnapshot {
    private static final String KEY_MIN_STACK_SIZE = "min_stack_size";
    private static final String KEY_SLOTS = "slots";
    private static final String KEY_SLOT = "slot";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_STACK = "stack";
    private static final int SLOT_COUNT = 16;

    private final long[] amounts;
    private final ItemStack[] markers;
    private final int minStackSize;

    private MEItemInventorySnapshot(ItemStack[] markers, long[] amounts, int minStackSize) {
        this.markers = markers;
        this.amounts = amounts;
        this.minStackSize = minStackSize;
    }

    public static MEItemInventorySnapshot empty() {
        ItemStack[] markers = new ItemStack[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            markers[slot] = ItemStack.EMPTY;
        }
        return new MEItemInventorySnapshot(markers, new long[SLOT_COUNT], 1);
    }

    public static MEItemInventorySnapshot from(MEItemInventoryBusState state, long[] availableAmounts) {
        if (availableAmounts.length != state.size()) {
            throw new IllegalArgumentException("Expected " + state.size() + " availability values");
        }

        ItemStack[] markers = new ItemStack[state.size()];
        long[] amounts = new long[state.size()];
        for (int slot = 0; slot < state.size(); slot++) {
            markers[slot] = state.getMarker(slot);
            amounts[slot] = Math.max(0L, availableAmounts[slot]);
        }
        return new MEItemInventorySnapshot(markers, amounts, state.getMinStackSize());
    }

    public static MEItemInventorySnapshot from(ItemStack[] sourceMarkers,
                                               int minimumStock,
                                               long[] availableAmounts) {
        if (sourceMarkers == null || sourceMarkers.length != SLOT_COUNT) {
            throw new IllegalArgumentException("Expected " + SLOT_COUNT + " marker slots");
        }
        if (availableAmounts == null || availableAmounts.length != sourceMarkers.length) {
            throw new IllegalArgumentException("Expected " + sourceMarkers.length + " availability values");
        }

        ItemStack[] markers = new ItemStack[SLOT_COUNT];
        long[] amounts = new long[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            markers[slot] = sourceMarkers[slot] == null ? ItemStack.EMPTY : sourceMarkers[slot].copy();
            markers[slot].setCount(markers[slot].isEmpty() ? 0 : 1);
            amounts[slot] = Math.max(0L, availableAmounts[slot]);
        }
        return new MEItemInventorySnapshot(markers, amounts, Math.max(1, minimumStock));
    }

    public static MEItemInventorySnapshot readNBT(NBTTagCompound tag) {
        ItemStack[] markers = new ItemStack[SLOT_COUNT];
        long[] amounts = new long[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            markers[slot] = ItemStack.EMPTY;
        }

        int minStackSize = Math.max(1, tag.getInteger(KEY_MIN_STACK_SIZE));
        NBTTagList slots = tag.getTagList(KEY_SLOTS, 10);
        for (int index = 0; index < slots.tagCount(); index++) {
            NBTTagCompound entry = slots.getCompoundTagAt(index);
            int slot = entry.getByte(KEY_SLOT) & 0xFF;
            if (slot >= SLOT_COUNT) continue;

            amounts[slot] = Math.max(0L, entry.getLong(KEY_AMOUNT));
            if (entry.hasKey(KEY_STACK, 10)) {
                ItemStack marker = new ItemStack(entry.getCompoundTag(KEY_STACK));
                if (!marker.isEmpty()) {
                    marker.setCount(1);
                    markers[slot] = marker;
                }
            }
        }
        return new MEItemInventorySnapshot(markers, amounts, minStackSize);
    }

    public int size() {
        return markers.length;
    }

    public int getMinStackSize() {
        return minStackSize;
    }

    public boolean sameAs(MEItemInventorySnapshot other) {
        if (other == null || minStackSize != other.minStackSize || amounts.length != other.amounts.length) {
            return false;
        }
        for (int slot = 0; slot < amounts.length; slot++) {
            if (amounts[slot] != other.amounts[slot]
                    || !ItemStack.areItemStacksEqual(markers[slot], other.markers[slot])) {
                return false;
            }
        }
        return true;
    }

    public long getAmount(int slot) {
        checkSlot(slot);
        return amounts[slot];
    }

    /**
     * Keeps quantities only for markers whose item identity has not changed.
     */
    public MEItemInventorySnapshot reconfigured(MEItemInventoryBusState state) {
        if (state.size() != markers.length) {
            throw new IllegalArgumentException("Expected " + markers.length + " marker slots");
        }

        long[] retainedAmounts = new long[markers.length];
        for (int slot = 0; slot < markers.length; slot++) {
            ItemStack currentMarker = state.getMarker(slot);
            if (ItemStack.areItemsEqual(markers[slot], currentMarker)
                    && ItemStack.areItemStackTagsEqual(markers[slot], currentMarker)) {
                retainedAmounts[slot] = amounts[slot];
            }
        }
        return from(state, retainedAmounts);
    }

    public MEItemInventorySnapshot reconfigured(ItemStack[] replacementMarkers, int replacementMinimumStock) {
        if (replacementMarkers == null || replacementMarkers.length != markers.length) {
            throw new IllegalArgumentException("Expected " + markers.length + " marker slots");
        }

        long[] retainedAmounts = new long[markers.length];
        for (int slot = 0; slot < markers.length; slot++) {
            ItemStack replacement = replacementMarkers[slot] == null
                    ? ItemStack.EMPTY
                    : replacementMarkers[slot];
            if (ItemStack.areItemsEqual(markers[slot], replacement)
                    && ItemStack.areItemStackTagsEqual(markers[slot], replacement)) {
                retainedAmounts[slot] = amounts[slot];
            }
        }
        return from(replacementMarkers, replacementMinimumStock, retainedAmounts);
    }

    public ItemStack getMarker(int slot) {
        checkSlot(slot);
        return markers[slot].copy();
    }

    public ItemStack getVirtualStack(int slot) {
        checkSlot(slot);
        ItemStack marker = markers[slot];
        if (marker.isEmpty() || amounts[slot] < minStackSize) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = marker.copy();
        stack.setCount((int) Math.min(amounts[slot], Integer.MAX_VALUE));
        return stack;
    }

    public void writeNBT(NBTTagCompound tag) {
        tag.setInteger(KEY_MIN_STACK_SIZE, minStackSize);
        NBTTagList slots = new NBTTagList();
        for (int slot = 0; slot < markers.length; slot++) {
            ItemStack marker = markers[slot];
            long amount = amounts[slot];
            if (marker.isEmpty() && amount == 0L) continue;

            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte(KEY_SLOT, (byte) slot);
            entry.setLong(KEY_AMOUNT, amount);
            if (!marker.isEmpty()) {
                entry.setTag(KEY_STACK, marker.writeToNBT(new NBTTagCompound()));
            }
            slots.appendTag(entry);
        }
        tag.setTag(KEY_SLOTS, slots);
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= markers.length) {
            throw new IndexOutOfBoundsException("Virtual slot out of range: " + slot);
        }
    }
}
