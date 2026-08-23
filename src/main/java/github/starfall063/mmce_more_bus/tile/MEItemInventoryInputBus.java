package github.starfall063.mmce_more_bus.tile;

import appeng.api.AEApi;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * AE item input bus with sixteen persistent item marker channels.
 */
public final class MEItemInventoryInputBus extends AbstractMarkerMEInputBus<ItemStack> {
    public static final int SLOT_COUNT = AbstractMarkerMEInputBus.SLOT_COUNT;
    private static final String KEY_STATE = "me_item_inventory_input_bus_state";
    private MEItemInventorySnapshot snapshot = MEItemInventorySnapshot.empty();    private final MEItemInventoryVirtualHandler virtualHandler = new MEItemInventoryVirtualHandler(
            MEItemInventorySnapshot.empty(),
            this::extractFromNetwork
    );

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.meItemInputBus);
    }

    @Override
    public MachineComponent.ItemBus provideComponent() {
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public long getGroupID() {
                return MEItemInventoryInputBus.this.getGroupId();
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public IItemHandlerModifiable getContainerProvider() {
                return virtualHandler;
            }
        };
    }

    @Override
    protected void refreshSnapshot() {
        long[] amounts = new long[SLOT_COUNT];
        if (!getProxy().isActive()) {
            publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
            return;
        }

        IStorageGrid storageGrid;
        try {
            storageGrid = getProxy().getStorage();
        } catch (GridAccessException ignored) {
            publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
            return;
        }
        if (storageGrid == null) {
            publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
            return;
        }

        IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IMEMonitor<IAEItemStack> monitor = storageGrid.getInventory(channel);
        if (monitor == null) {
            publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
            return;
        }

        IItemList<IAEItemStack> stored = monitor.getStorageList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack marker = markerAt(slot);
            if (marker.isEmpty()) continue;

            IAEItemStack request = channel.createStack(marker);
            IAEItemStack entry = request == null ? null : stored.findPrecise(request);
            if (entry != null) amounts[slot] = Math.max(0L, entry.getStackSize());
        }
        publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
    }

    private void publishSnapshot(MEItemInventorySnapshot replacement) {
        if (snapshot.sameAs(replacement)) return;
        snapshot = replacement;
        virtualHandler.setSnapshot(replacement);
        markForUpdateSync();
    }

    private ItemStack extractFromNetwork(int slot, int amount) {
        ItemStack requested = snapshot.getVirtualStack(slot);
        if (requested.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        requested = requested.copy();
        requested.setCount(Math.min(amount, requested.getCount()));

        try {
            IStorageGrid storageGrid = getProxy().getStorage();
            if (storageGrid == null) return ItemStack.EMPTY;

            IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> monitor = storageGrid.getInventory(channel);
            if (monitor == null) return ItemStack.EMPTY;

            IAEItemStack request = channel.createStack(requested);
            if (request == null) return ItemStack.EMPTY;

            IAEItemStack extracted = appeng.util.Platform.poweredExtraction(
                    getProxy().getEnergy(), monitor, request, source
            );
            if (extracted == null) return ItemStack.EMPTY;

            ItemStack result = extracted.createItemStack();
            refreshSnapshot();
            return result;
        } catch (GridAccessException ignored) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (compound.hasKey(KEY_STATE, 10)) readMarkerState(compound.getCompoundTag(KEY_STATE));
        snapshot = MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), new long[SLOT_COUNT]);
        virtualHandler.setSnapshot(snapshot);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        NBTTagCompound stateTag = new NBTTagCompound();
        writeMarkerState(stateTag);
        compound.setTag(KEY_STATE, stateTag);
    }

    @Override
    public void readNetNBT(NBTTagCompound compound) {
        super.readNetNBT(compound);
        if (!compound.hasKey("me_item_inventory_input_bus_snapshot", 10)) return;

        snapshot = MEItemInventorySnapshot.readNBT(
                compound.getCompoundTag("me_item_inventory_input_bus_snapshot")
        );
        Object[] markers = new Object[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) markers[slot] = snapshot.getMarker(slot);
        replaceMarkerState(markers, snapshot.getMinStackSize());
        virtualHandler.setSnapshot(snapshot);
    }

    @Override
    public void writeNetNBT(NBTTagCompound compound) {
        super.writeNetNBT(compound);
        NBTTagCompound snapshotTag = new NBTTagCompound();
        snapshot.writeNBT(snapshotTag);
        compound.setTag("me_item_inventory_input_bus_snapshot", snapshotTag);
    }

    @Override
    protected boolean hasActiveConfiguration() {
        return hasMarkers();
    }

    @Override
    protected int getMinimumPollingInterval() {
        return MEItemInventoryInputBusConfig.minPollingInterval(
                MEItemInventoryInputBusConfig.POLLING.minimumPollingInterval,
                MEItemInventoryInputBusConfig.POLLING.maximumPollingInterval
        );
    }

    @Override
    protected int getMaximumPollingInterval() {
        return MEItemInventoryInputBusConfig.maxPollingInterval(
                MEItemInventoryInputBusConfig.POLLING.minimumPollingInterval,
                MEItemInventoryInputBusConfig.POLLING.maximumPollingInterval
        );
    }

    @Override
    protected void invalidateResourceSnapshot() {
        publishSnapshot(snapshot.reconfigured(currentMarkers(), getMinStackSize()));
    }

    @Override
    protected void markerStateLoaded() {
        publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), new long[SLOT_COUNT]));
    }

    public ItemStack getVirtualStack(int slot) {
        return virtualHandler.getStackInSlot(slot);
    }

    public long getVirtualAmount(int slot) {
        return snapshot.getAmount(slot);
    }

    public IItemHandlerModifiable getVirtualHandler() {
        return virtualHandler;
    }

    @Override
    protected ItemStack emptyMarker() {
        return ItemStack.EMPTY;
    }

    @Override
    protected ItemStack copyIdentity(ItemStack marker) {
        if (marker == null || marker.isEmpty()) return ItemStack.EMPTY;
        ItemStack identity = marker.copy();
        identity.setCount(1);
        return identity;
    }

    @Override
    protected boolean isEmpty(ItemStack marker) {
        return marker == null || marker.isEmpty();
    }

    @Override
    protected boolean sameIdentity(ItemStack first, ItemStack second) {
        return !isEmpty(first)
                && !isEmpty(second)
                && ItemStack.areItemsEqual(first, second)
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    @Override
    protected void writeMarker(ItemStack marker, NBTTagCompound tag) {
        marker.writeToNBT(tag);
    }

    @Override
    protected ItemStack readMarker(NBTTagCompound tag) {
        return new ItemStack(tag);
    }

    @Override
    protected String markerListKey() {
        return "sfc_me_markers";
    }

    @Override
    protected String minimumStockKey() {
        return "sfc_me_min_stack_size";
    }

    private ItemStack[] currentMarkers() {
        ItemStack[] markers = new ItemStack[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) markers[slot] = markerAt(slot);
        return markers;
    }


}
