package github.starfall063.mmce_more_bus.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared marker and minimum-stock state for typed ME input buses.
 *
 * <p>The concrete bus owns its resource snapshot and virtual handler. This
 * class only owns the configuration that identifies the resources to expose.</p>
 */
public abstract class AbstractMarkerMEInputBus<M> extends AbstractMEInputBus {
    public static final int SLOT_COUNT = 16;

    private static final String DEFAULT_MARKER_LIST_KEY = "markers";
    private static final String DEFAULT_MINIMUM_STOCK_KEY = "min_stack_size";
    private static final String DEFAULT_SLOT_KEY = "Slot";
    private static final String DEFAULT_MARKER_TAG_KEY = "Stack";

    private final Object[] markers = new Object[SLOT_COUNT];
    private int minStackSize = 1;

    protected AbstractMarkerMEInputBus() {
        clearMarkersInternal();
    }

    private static int normalizeMinimumStock(int value) {
        return Math.max(1, value);
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Marker slot out of range: " + slot);
        }
    }

    public final int size() {
        return SLOT_COUNT;
    }

    public final M getMarker(int slot) {
        checkSlot(slot);
        return copyIdentity(markerAt(slot));
    }

    public final boolean setMarker(int slot, M marker) {
        checkSlot(slot);
        M identity = copyIdentity(marker);
        if (isEmpty(identity)) {
            if (isEmpty(markerAt(slot))) return true;
            markers[slot] = emptyMarker();
            configurationChanged();
            return true;
        }
        if (containsIdentityInOtherSlot(slot, identity)) return false;

        M previous = markerAt(slot);
        if (sameIdentity(previous, identity)) return true;

        markers[slot] = identity;
        configurationChanged();
        return true;
    }

    public final void clearMarker(int slot) {
        checkSlot(slot);
        if (isEmpty(markerAt(slot))) return;
        markers[slot] = emptyMarker();
        configurationChanged();
    }

    public final boolean hasMarkers() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!isEmpty(markerAt(slot))) return true;
        }
        return false;
    }

    public final int getMinStackSize() {
        return minStackSize;
    }

    public final void setMinStackSize(int value) {
        int normalized = normalizeMinimumStock(value);
        if (minStackSize == normalized) return;
        minStackSize = normalized;
        configurationChanged();
    }

    /**
     * Returns a defensive identity copy for resource-specific snapshot code.
     */
    protected final M markerAt(int slot) {
        checkSlot(slot);
        @SuppressWarnings("unchecked")
        M marker = (M) markers[slot];
        return copyIdentity(marker);
    }

    /**
     * Clears all configured markers without notifying the grid.
     */
    protected final void clearAllMarkers() {
        boolean changed = hasMarkers();
        clearMarkersInternal();
        if (changed) configurationChanged();
    }

    /**
     * Clears all markers while a snapshot is being rebuilt or loaded.
     */
    protected final void clearAllMarkersSilently() {
        clearMarkersInternal();
    }

    /**
     * Sets one marker while a generated marker set is being rebuilt.
     */
    protected final boolean setMarkerSilently(int slot, M marker) {
        checkSlot(slot);
        M identity = copyIdentity(marker);
        if (isEmpty(identity) || containsIdentityInOtherSlot(slot, identity)) return false;
        markers[slot] = identity;
        return true;
    }

    /**
     * Replaces marker identities without notifying the grid.
     */
    protected final void replaceMarkerState(Object[] replacement, int replacementMinimumStock) {
        if (replacement == null || replacement.length != SLOT_COUNT) {
            throw new IllegalArgumentException("Expected " + SLOT_COUNT + " marker slots");
        }
        clearMarkersInternal();
        minStackSize = normalizeMinimumStock(replacementMinimumStock);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            @SuppressWarnings("unchecked")
            M marker = (M) replacement[slot];
            M identity = copyIdentity(marker);
            if (isEmpty(identity) || containsIdentityInOtherSlot(slot, identity)) continue;
            markers[slot] = identity;
        }
    }

    /**
     * Reads a marker configuration without refreshing the resource snapshot.
     */
    protected final void readMarkerState(NBTTagCompound compound) {
        clearMarkersInternal();
        minStackSize = normalizeMinimumStock(compound.getInteger(minimumStockKey()));

        NBTTagList stored = compound.getTagList(markerListKey(), 10);
        List<NBTTagCompound> entries = new ArrayList<>();
        for (int index = 0; index < stored.tagCount(); index++) {
            entries.add(stored.getCompoundTagAt(index));
        }
        entries.sort(Comparator.comparingInt(entry -> entry.getByte(markerSlotKey()) & 0xFF));

        for (NBTTagCompound entry : entries) {
            int slot = entry.getByte(markerSlotKey()) & 0xFF;
            if (slot >= SLOT_COUNT || !entry.hasKey(markerTagKey(), 10)) continue;

            M marker = copyIdentity(readMarker(entry.getCompoundTag(markerTagKey())));
            if (isEmpty(marker) || containsIdentityInOtherSlot(slot, marker)) continue;
            markers[slot] = marker;
        }
        markerStateLoaded();
    }

    /**
     * Writes only configured markers in slot order and the normalized minimum stock.
     */
    protected final void writeMarkerState(NBTTagCompound compound) {
        NBTTagList stored = new NBTTagList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            M marker = markerAt(slot);
            if (isEmpty(marker)) continue;

            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte(markerSlotKey(), (byte) slot);
            NBTTagCompound markerTag = new NBTTagCompound();
            writeMarker(marker, markerTag);
            entry.setTag(markerTagKey(), markerTag);
            stored.appendTag(entry);
        }
        compound.setTag(markerListKey(), stored);
        compound.setInteger(minimumStockKey(), minStackSize);
    }

    protected String markerListKey() {
        return DEFAULT_MARKER_LIST_KEY;
    }

    protected String minimumStockKey() {
        return DEFAULT_MINIMUM_STOCK_KEY;
    }

    protected String markerSlotKey() {
        return DEFAULT_SLOT_KEY;
    }

    protected String markerTagKey() {
        return DEFAULT_MARKER_TAG_KEY;
    }

    protected abstract M emptyMarker();

    protected abstract M copyIdentity(M marker);

    protected abstract boolean isEmpty(M marker);

    protected abstract boolean sameIdentity(M first, M second);

    protected abstract void writeMarker(M marker, NBTTagCompound tag);

    protected abstract M readMarker(NBTTagCompound tag);

    /**
     * Invalidates the immutable resource snapshot after configuration changes.
     */
    protected abstract void invalidateResourceSnapshot();

    /**
     * Rebuilds a fresh zero-availability snapshot after persistent state loads.
     */
    protected void markerStateLoaded() {
        invalidateResourceSnapshot();
    }

    private void configurationChanged() {
        invalidateResourceSnapshot();
        markDirty();
        alertTickingDevice();
        if (getWorld() != null) markForUpdateSync();
    }

    private boolean containsIdentityInOtherSlot(int excludedSlot, M candidate) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (slot != excludedSlot && sameIdentity(markerAt(slot), candidate)) return true;
        }
        return false;
    }

    private void clearMarkersInternal() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            markers[slot] = emptyMarker();
        }
    }
}
