package github.starfall063.mmce_more_bus.tile;

import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import com.mekeng.github.common.me.storage.impl.GasStorageChannel;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class MEGasInventoryInputBus extends AbstractMarkerMEInputBus<GasStack> {
    public static final int SLOT_COUNT = AbstractMarkerMEInputBus.SLOT_COUNT;
    private static final String KEY_MARKERS = "sfc_me_gas_markers";
    private static final String KEY_PREVIEW = "sfc_me_gas_preview";
    private static final String KEY_MIN_STACK_SIZE = "sfc_me_gas_min_stack_size";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_STACK = "Stack";
    private static final String KEY_AMOUNT = "Amount";
    private final VirtualGasHandler virtualHandler;
    private GasSnapshot snapshot = GasSnapshot.empty();

    public MEGasInventoryInputBus() {
        this(null);
    }

    MEGasInventoryInputBus(GasExtractor extractor) {
        virtualHandler = new VirtualGasHandler(
                snapshot,
                extractor == null ? this::extractFromNetwork : extractor,
                this::publishSnapshot,
                this::getMinStackSize
        );
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Gas marker slot out of range: " + slot);
        }
    }

    private static int clampMinStackSize(int value) {
        return Math.max(1, value);
    }

    private static long clampVirtualAmount(long value) {
        return Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.meGasInputBus);
    }

    @Override
    public MachineComponent<IExtendedGasHandler> provideComponent() {
        return new MachineComponent<IExtendedGasHandler>(IOType.INPUT) {
            @Override
            public long getGroupID() {
                return MEGasInventoryInputBus.this.getGroupId();
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_GAS;
            }

            @Override
            public IExtendedGasHandler getContainerProvider() {
                return virtualHandler;
            }
        };
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readMarkerState(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        writeMarkerState(compound);
    }

    @Override
    public void readNetNBT(NBTTagCompound compound) {
        super.readNetNBT(compound);
        readPreviewState(compound);
    }

    @Override
    public void writeNetNBT(NBTTagCompound compound) {
        super.writeNetNBT(compound);
        writePreviewState(compound);
    }

    void readPreviewState(NBTTagCompound compound) {
        if (!compound.hasKey(KEY_PREVIEW, 10)) return;

        GasSnapshot replacement = GasSnapshot.readNBT(compound.getCompoundTag(KEY_PREVIEW));
        Object[] replacementMarkers = new Object[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) replacementMarkers[slot] = replacement.getMarker(slot);
        replaceMarkerState(replacementMarkers, compound.getInteger(KEY_MIN_STACK_SIZE));
        snapshot = replacement;
        virtualHandler.setSnapshot(replacement);
    }

    void writePreviewState(NBTTagCompound compound) {
        NBTTagCompound preview = new NBTTagCompound();
        snapshot.writeNBT(preview);
        compound.setTag(KEY_PREVIEW, preview);
        compound.setInteger(KEY_MIN_STACK_SIZE, getMinStackSize());
    }

    void publishSnapshot(int slot, long amount) {
        checkSlot(slot);
        publishSnapshot(snapshot.withAmount(slot, amount));
    }

    public long getVirtualAmount(int slot) {
        return snapshot.getAmount(slot);
    }

    public IExtendedGasHandler getVirtualHandler() {
        return virtualHandler;
    }

    private void reconfigureSnapshot() {
        publishSnapshot(snapshot.reconfigured(currentMarkers()));
    }

    private void resetSnapshot() {
        GasSnapshot replacement = GasSnapshot.from(currentMarkers(), new long[SLOT_COUNT]);
        snapshot = replacement;
        virtualHandler.setSnapshot(replacement);
    }

    private void publishSnapshot(GasSnapshot replacement) {
        if (snapshot.sameAs(replacement)) return;
        snapshot = replacement;
        virtualHandler.setSnapshot(replacement);
        if (getWorld() != null) markForUpdateSync();
    }

    @Override
    protected boolean hasActiveConfiguration() {
        return hasMarkers();
    }

    @Override
    protected void refreshSnapshot() {
        long[] amounts = new long[SLOT_COUNT];
        GasStack[] markers = currentMarkers();
        if (!getProxy().isActive()) {
            publishSnapshot(GasSnapshot.from(markers, amounts));
            return;
        }

        IStorageGrid storageGrid;
        try {
            storageGrid = getProxy().getStorage();
        } catch (GridAccessException ignored) {
            publishSnapshot(GasSnapshot.from(markers, amounts));
            return;
        }
        if (storageGrid == null) {
            publishSnapshot(GasSnapshot.from(markers, amounts));
            return;
        }

        IGasStorageChannel channel = GasStorageChannel.INSTANCE;
        IMEMonitor<IAEGasStack> monitor = storageGrid.getInventory(channel);
        if (monitor == null) {
            publishSnapshot(GasSnapshot.from(markers, amounts));
            return;
        }

        IItemList<IAEGasStack> stored = monitor.getStorageList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            GasStack marker = markers[slot];
            if (marker == null || marker.getGas() == null) continue;

            IAEGasStack request = channel.createStack(marker);
            IAEGasStack entry = request == null ? null : stored.findPrecise(request);
            if (entry != null) amounts[slot] = Math.max(0L, entry.getStackSize());
        }
        publishSnapshot(GasSnapshot.from(markers, amounts));
    }

    private GasStack extractFromNetwork(int slot, GasStack request) {
        if (request == null || request.getGas() == null || request.amount <= 0) return null;

        try {
            IStorageGrid storageGrid = getProxy().getStorage();
            if (storageGrid == null) return null;

            IGasStorageChannel channel = GasStorageChannel.INSTANCE;
            IMEMonitor<IAEGasStack> monitor = storageGrid.getInventory(channel);
            if (monitor == null) return null;

            IAEGasStack aeRequest = AEGasStack.of(request);
            if (aeRequest == null) return null;

            IAEGasStack extracted = appeng.util.Platform.poweredExtraction(
                    getProxy().getEnergy(), monitor, aeRequest, source
            );
            if (extracted == null) return null;

            refreshSnapshot();
            return extracted.getGasStack();
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    @Override
    protected void invalidateResourceSnapshot() {
        reconfigureSnapshot();
    }

    @Override
    protected void markerStateLoaded() {
        resetSnapshot();
    }

    @Override
    protected GasStack emptyMarker() {
        return null;
    }

    @Override
    protected GasStack copyIdentity(GasStack marker) {
        return GasSnapshot.copyIdentity(marker);
    }

    @Override
    protected boolean isEmpty(GasStack marker) {
        return marker == null || marker.getGas() == null;
    }

    @Override
    protected boolean sameIdentity(GasStack first, GasStack second) {
        return !isEmpty(first) && !isEmpty(second) && first.isGasEqual(second);
    }

    @Override
    protected void writeMarker(GasStack marker, NBTTagCompound tag) {
        marker.write(tag);
    }

    @Override
    protected GasStack readMarker(NBTTagCompound tag) {
        return GasStack.readFromNBT(tag);
    }

    @Override
    protected String markerListKey() {
        return KEY_MARKERS;
    }

    @Override
    protected String minimumStockKey() {
        return KEY_MIN_STACK_SIZE;
    }

    private GasStack[] currentMarkers() {
        GasStack[] markers = new GasStack[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) markers[slot] = markerAt(slot);
        return markers;
    }

    @FunctionalInterface
    interface GasExtractor {
        GasStack extract(int slot, GasStack request);
    }

    private static final class GasSnapshot {
        private final GasStack[] markers;
        private final long[] amounts;

        private GasSnapshot(GasStack[] markers, long[] amounts) {
            this.markers = markers;
            this.amounts = amounts;
        }

        private static GasSnapshot empty() {
            return new GasSnapshot(new GasStack[SLOT_COUNT], new long[SLOT_COUNT]);
        }

        private static GasStack copyIdentity(GasStack stack) {
            if (stack == null || stack.getGas() == null) return null;
            GasStack identity = stack.copy();
            identity.amount = 1;
            return identity;
        }

        private static GasSnapshot from(GasStack[] sourceMarkers, long[] sourceAmounts) {
            if (sourceAmounts.length != SLOT_COUNT) {
                throw new IllegalArgumentException("Expected " + SLOT_COUNT + " availability values");
            }

            GasStack[] markerCopies = new GasStack[SLOT_COUNT];
            long[] amountCopies = new long[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                markerCopies[slot] = copyIdentity(sourceMarkers[slot]);
                amountCopies[slot] = clampVirtualAmount(sourceAmounts[slot]);
            }
            return new GasSnapshot(markerCopies, amountCopies);
        }

        private static GasSnapshot readNBT(NBTTagCompound compound) {
            GasStack[] markers = new GasStack[SLOT_COUNT];
            long[] amounts = new long[SLOT_COUNT];
            NBTTagList stored = compound.getTagList(KEY_MARKERS, 10);
            for (int index = 0; index < stored.tagCount(); index++) {
                NBTTagCompound entry = stored.getCompoundTagAt(index);
                int slot = entry.getByte(KEY_SLOT) & 0xFF;
                if (slot >= SLOT_COUNT || !entry.hasKey(KEY_STACK, 10)) continue;

                GasStack marker = copyIdentity(GasStack.readFromNBT(entry.getCompoundTag(KEY_STACK)));
                if (marker == null) continue;

                markers[slot] = marker;
                amounts[slot] = clampVirtualAmount(entry.getLong(KEY_AMOUNT));
            }
            return new GasSnapshot(markers, amounts);
        }

        private boolean sameAs(GasSnapshot other) {
            if (other == null) return false;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (amounts[slot] != other.amounts[slot]) return false;
                GasStack first = markers[slot];
                GasStack second = other.markers[slot];
                if (first == null ? second != null : !first.isGasEqual(second)) return false;
            }
            return true;
        }

        private GasSnapshot withAmount(int slot, long amount) {
            long[] replacement = amounts.clone();
            replacement[slot] = clampVirtualAmount(amount);
            return new GasSnapshot(markers, replacement);
        }

        private GasSnapshot reconfigured(GasStack[] replacementMarkers) {
            long[] retainedAmounts = new long[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack marker = replacementMarkers[slot];
                if (marker != null && markers[slot] != null && marker.isGasEqual(markers[slot])) {
                    retainedAmounts[slot] = amounts[slot];
                }
            }
            return from(replacementMarkers, retainedAmounts);
        }

        private long getAmount(int slot) {
            checkSlot(slot);
            return amounts[slot];
        }

        private GasStack getMarker(int slot) {
            checkSlot(slot);
            return copyIdentity(markers[slot]);
        }

        private GasStack getGas(int slot) {
            checkSlot(slot);
            GasStack marker = markers[slot];
            if (marker == null || amounts[slot] <= 0L) return null;

            GasStack stack = marker.copy();
            stack.amount = (int) Math.min(amounts[slot], Integer.MAX_VALUE);
            return stack;
        }

        private GasSnapshot withoutDrawnAmount(int slot, GasStack identity, int amount) {
            GasStack marker = markers[slot];
            if (marker == null || !marker.isGasEqual(identity)) return this;
            return withAmount(slot, Math.max(0L, amounts[slot] - amount));
        }

        private void writeNBT(NBTTagCompound compound) {
            NBTTagList stored = new NBTTagList();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack marker = markers[slot];
                if (marker == null) continue;

                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte(KEY_SLOT, (byte) slot);
                entry.setLong(KEY_AMOUNT, amounts[slot]);
                entry.setTag(KEY_STACK, marker.write(new NBTTagCompound()));
                stored.appendTag(entry);
            }
            compound.setTag(KEY_MARKERS, stored);
        }
    }

    private static final class VirtualGasHandler implements IExtendedGasHandler {
        private final AtomicReference<GasSnapshot> snapshot;
        private final GasExtractor extractor;
        private final Consumer<GasSnapshot> publisher;
        private final IntSupplier minimumStock;

        private VirtualGasHandler(
                GasSnapshot initialSnapshot,
                GasExtractor extractor,
                Consumer<GasSnapshot> publisher,
                IntSupplier minimumStock
        ) {
            this.snapshot = new AtomicReference<>(initialSnapshot);
            this.extractor = extractor;
            this.publisher = publisher;
            this.minimumStock = minimumStock;
        }

        private void setSnapshot(GasSnapshot replacement) {
            snapshot.set(replacement);
        }

        @Override
        public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
            return 0;
        }

        @Override
        public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
            if (amount <= 0) return null;

            GasSnapshot current = snapshot.get();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack available = current.getGas(slot);
                if (available == null) continue;
                if (available.amount < minimumStock.getAsInt()) continue;

                available.amount = Math.min(available.amount, amount);
                return doTransfer ? extract(slot, current, available) : available;
            }
            return null;
        }

        @Override
        public GasStack drawGas(GasStack request, boolean doTransfer) {
            if (request == null || request.getGas() == null || request.amount <= 0) return null;

            GasSnapshot current = snapshot.get();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack available = current.getGas(slot);
                if (available == null || !available.isGasEqual(request)) continue;
                if (available.amount < minimumStock.getAsInt()) return null;

                available.amount = Math.min(available.amount, request.amount);
                return doTransfer ? extract(slot, current, available) : available;
            }
            return null;
        }

        @Override
        public boolean canReceiveGas(EnumFacing side, Gas gas) {
            return false;
        }

        @Override
        public boolean canDrawGas(EnumFacing side, Gas gas) {
            if (gas == null) return false;
            GasSnapshot current = snapshot.get();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack available = current.getGas(slot);
                if (available != null && available.isGasEqual(gas)) return true;
            }
            return false;
        }

        @Override
        public GasTankInfo[] getTankInfo() {
            GasSnapshot current = snapshot.get();
            GasTankInfo[] tanks = new GasTankInfo[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack gas = current.getGas(slot);
                tanks[slot] = new GasTankInfo() {
                    @Override
                    public GasStack getGas() {
                        return gas == null ? null : gas.copy();
                    }

                    @Override
                    public int getStored() {
                        return gas == null ? 0 : gas.amount;
                    }

                    @Override
                    public int getMaxGas() {
                        return Integer.MAX_VALUE;
                    }
                };
            }
            return tanks;
        }

        private GasStack extract(int slot, GasSnapshot current, GasStack request) {
            GasStack extracted = extractor.extract(slot, request.copy());
            if (extracted == null || !extracted.isGasEqual(request) || extracted.amount <= 0) return null;

            extracted.amount = Math.min(extracted.amount, request.amount);
            publisher.accept(current.withoutDrawnAmount(slot, request, extracted.amount));
            return extracted;
        }
    }
}
