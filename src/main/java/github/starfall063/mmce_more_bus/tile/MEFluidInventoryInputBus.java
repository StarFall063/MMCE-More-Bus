package github.starfall063.mmce_more_bus.tile;

import appeng.api.AEApi;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class MEFluidInventoryInputBus extends AbstractMarkerMEInputBus<FluidStack> {
    public static final int SLOT_COUNT = AbstractMarkerMEInputBus.SLOT_COUNT;
    private static final String KEY_MARKERS = "sfc_me_fluid_markers";
    private static final String KEY_PREVIEW = "sfc_me_fluid_preview";
    private static final String KEY_MIN_STACK_SIZE = "sfc_me_fluid_min_stack_size";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_STACK = "Stack";
    private static final String KEY_AMOUNT = "Amount";
    private final VirtualFluidHandler virtualHandler;
    private FluidSnapshot snapshot = FluidSnapshot.empty();

    public MEFluidInventoryInputBus() {
        this(null);
    }

    MEFluidInventoryInputBus(FluidExtractor extractor) {
        virtualHandler = new VirtualFluidHandler(
                snapshot,
                extractor == null ? this::extractFromNetwork : extractor,
                this::publishSnapshot,
                this::getMinStackSize
        );
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Fluid marker slot out of range: " + slot);
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
        return new ItemStack(ItemsMM.meFluidInputBus);
    }

    @Override
    public MachineComponent.FluidHatch provideComponent() {
        return new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override
            public long getGroupID() {
                return MEFluidInventoryInputBus.this.getGroupId();
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public IFluidHandler getContainerProvider() {
                return virtualHandler;
            }
        };
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readMarkerState(compound);
        resetSnapshot();
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

        FluidSnapshot replacement = FluidSnapshot.readNBT(compound.getCompoundTag(KEY_PREVIEW));
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

    public IFluidHandler getVirtualHandler() {
        return virtualHandler;
    }

    private void reconfigureSnapshot() {
        publishSnapshot(snapshot.reconfigured(currentMarkers()));
    }

    private void resetSnapshot() {
        FluidSnapshot replacement = FluidSnapshot.from(currentMarkers(), new long[SLOT_COUNT]);
        snapshot = replacement;
        virtualHandler.setSnapshot(replacement);
    }

    private void publishSnapshot(FluidSnapshot replacement) {
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
        FluidStack[] markers = currentMarkers();
        if (!getProxy().isActive()) {
            publishSnapshot(FluidSnapshot.from(markers, amounts));
            return;
        }

        IStorageGrid storageGrid;
        try {
            storageGrid = getProxy().getStorage();
        } catch (GridAccessException ignored) {
            publishSnapshot(FluidSnapshot.from(markers, amounts));
            return;
        }
        if (storageGrid == null) {
            publishSnapshot(FluidSnapshot.from(markers, amounts));
            return;
        }

        IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        IMEMonitor<IAEFluidStack> monitor = storageGrid.getInventory(channel);
        if (monitor == null) {
            publishSnapshot(FluidSnapshot.from(markers, amounts));
            return;
        }

        IItemList<IAEFluidStack> stored = monitor.getStorageList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            FluidStack marker = markers[slot];
            if (marker == null || marker.getFluid() == null) continue;

            IAEFluidStack request = channel.createStack(marker);
            IAEFluidStack entry = request == null ? null : stored.findPrecise(request);
            if (entry != null) amounts[slot] = Math.max(0L, entry.getStackSize());
        }
        publishSnapshot(FluidSnapshot.from(markers, amounts));
    }

    private FluidStack extractFromNetwork(int slot, FluidStack request) {
        if (request == null || request.amount <= 0) return null;

        try {
            IStorageGrid storageGrid = getProxy().getStorage();
            if (storageGrid == null) return null;

            IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
            IMEMonitor<IAEFluidStack> monitor = storageGrid.getInventory(channel);
            if (monitor == null) return null;

            IAEFluidStack aeRequest = channel.createStack(request);
            if (aeRequest == null) return null;

            IAEFluidStack extracted = appeng.util.Platform.poweredExtraction(
                    getProxy().getEnergy(), monitor, aeRequest, source
            );
            return extracted == null ? null : extracted.getFluidStack();
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
    protected FluidStack emptyMarker() {
        return null;
    }

    @Override
    protected FluidStack copyIdentity(FluidStack marker) {
        return FluidSnapshot.copyIdentity(marker);
    }

    @Override
    protected boolean isEmpty(FluidStack marker) {
        return marker == null || marker.getFluid() == null;
    }

    @Override
    protected boolean sameIdentity(FluidStack first, FluidStack second) {
        return !isEmpty(first) && !isEmpty(second) && first.isFluidEqual(second);
    }

    @Override
    protected void writeMarker(FluidStack marker, NBTTagCompound tag) {
        marker.writeToNBT(tag);
    }

    @Override
    protected FluidStack readMarker(NBTTagCompound tag) {
        return FluidStack.loadFluidStackFromNBT(tag);
    }

    @Override
    protected String markerListKey() {
        return KEY_MARKERS;
    }

    @Override
    protected String minimumStockKey() {
        return KEY_MIN_STACK_SIZE;
    }

    private FluidStack[] currentMarkers() {
        FluidStack[] markers = new FluidStack[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) markers[slot] = markerAt(slot);
        return markers;
    }

    @FunctionalInterface
    interface FluidExtractor {
        FluidStack extract(int slot, FluidStack request);
    }

    private static final class FluidSnapshot {
        private final FluidStack[] markers;
        private final long[] amounts;

        private FluidSnapshot(FluidStack[] markers, long[] amounts) {
            this.markers = markers;
            this.amounts = amounts;
        }

        private static FluidSnapshot empty() {
            return new FluidSnapshot(new FluidStack[SLOT_COUNT], new long[SLOT_COUNT]);
        }

        private static FluidSnapshot from(FluidStack[] sourceMarkers, long[] sourceAmounts) {
            if (sourceAmounts.length != SLOT_COUNT) {
                throw new IllegalArgumentException("Expected " + SLOT_COUNT + " availability values");
            }

            FluidStack[] markerCopies = new FluidStack[SLOT_COUNT];
            long[] amountCopies = new long[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                markerCopies[slot] = copyIdentity(sourceMarkers[slot]);
                amountCopies[slot] = clampVirtualAmount(sourceAmounts[slot]);
            }
            return new FluidSnapshot(markerCopies, amountCopies);
        }

        private static FluidStack copyIdentity(FluidStack stack) {
            if (stack == null || stack.getFluid() == null) return null;
            FluidStack identity = stack.copy();
            identity.amount = 1;
            return identity;
        }

        private static FluidSnapshot readNBT(NBTTagCompound compound) {
            FluidStack[] markers = new FluidStack[SLOT_COUNT];
            long[] amounts = new long[SLOT_COUNT];
            NBTTagList stored = compound.getTagList(KEY_MARKERS, 10);
            for (int index = 0; index < stored.tagCount(); index++) {
                NBTTagCompound entry = stored.getCompoundTagAt(index);
                int slot = entry.getByte(KEY_SLOT) & 0xFF;
                if (slot >= SLOT_COUNT || !entry.hasKey(KEY_STACK, 10)) continue;

                FluidStack marker = FluidStack.loadFluidStackFromNBT(entry.getCompoundTag(KEY_STACK));
                marker = copyIdentity(marker);
                if (marker == null) continue;

                markers[slot] = marker;
                amounts[slot] = clampVirtualAmount(entry.getLong(KEY_AMOUNT));
            }
            return new FluidSnapshot(markers, amounts);
        }

        private boolean sameAs(FluidSnapshot other) {
            if (other == null) return false;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (amounts[slot] != other.amounts[slot]) return false;
                FluidStack first = markers[slot];
                FluidStack second = other.markers[slot];
                if (first == null ? second != null : !first.isFluidEqual(second)) return false;
            }
            return true;
        }

        private FluidSnapshot withAmount(int slot, long amount) {
            long[] replacement = amounts.clone();
            replacement[slot] = clampVirtualAmount(amount);
            return new FluidSnapshot(markers, replacement);
        }

        private FluidSnapshot withoutDrainedAmount(int slot, FluidStack identity, int amount) {
            FluidStack marker = markers[slot];
            if (marker == null || !marker.isFluidEqual(identity)) return this;
            return withAmount(slot, Math.max(0L, amounts[slot] - amount));
        }

        private FluidSnapshot reconfigured(FluidStack[] replacementMarkers) {
            long[] retainedAmounts = new long[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                FluidStack marker = replacementMarkers[slot];
                if (marker != null && markers[slot] != null && marker.isFluidEqual(markers[slot])) {
                    retainedAmounts[slot] = amounts[slot];
                }
            }
            return from(replacementMarkers, retainedAmounts);
        }

        private long getAmount(int slot) {
            checkSlot(slot);
            return amounts[slot];
        }

        private FluidStack getMarker(int slot) {
            checkSlot(slot);
            return copyIdentity(markers[slot]);
        }

        private FluidStack getFluid(int slot) {
            checkSlot(slot);
            FluidStack marker = markers[slot];
            if (marker == null || amounts[slot] <= 0L) return null;

            FluidStack stack = marker.copy();
            stack.amount = (int) Math.min(amounts[slot], Integer.MAX_VALUE);
            return stack;
        }

        private void writeNBT(NBTTagCompound compound) {
            NBTTagList stored = new NBTTagList();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                FluidStack marker = markers[slot];
                if (marker == null) continue;

                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte(KEY_SLOT, (byte) slot);
                entry.setLong(KEY_AMOUNT, amounts[slot]);
                entry.setTag(KEY_STACK, marker.writeToNBT(new NBTTagCompound()));
                stored.appendTag(entry);
            }
            compound.setTag(KEY_MARKERS, stored);
        }
    }

    private static final class VirtualFluidHandler implements IFluidHandler {
        private final AtomicReference<FluidSnapshot> snapshot;
        private final FluidExtractor extractor;
        private final Consumer<FluidSnapshot> publisher;
        private final IntSupplier minimumStock;

        private VirtualFluidHandler(
                FluidSnapshot initialSnapshot,
                FluidExtractor extractor,
                Consumer<FluidSnapshot> publisher,
                IntSupplier minimumStock
        ) {
            this.snapshot = new AtomicReference<>(initialSnapshot);
            this.extractor = extractor;
            this.publisher = publisher;
            this.minimumStock = minimumStock;
        }

        private void setSnapshot(FluidSnapshot replacement) {
            snapshot.set(replacement);
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            FluidSnapshot current = snapshot.get();
            IFluidTankProperties[] properties = new IFluidTankProperties[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                properties[slot] = new FluidTankProperties(
                        current.getFluid(slot), Integer.MAX_VALUE, false, true
                );
            }
            return properties;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.amount <= 0) return null;

            FluidSnapshot current = snapshot.get();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                FluidStack available = current.getFluid(slot);
                if (available == null || !available.isFluidEqual(resource)) continue;
                if (available.amount < minimumStock.getAsInt()) return null;

                available.amount = Math.min(available.amount, resource.amount);
                if (doDrain) return extract(slot, current, available);
                return available;
            }
            return null;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0) return null;

            FluidSnapshot current = snapshot.get();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                FluidStack available = current.getFluid(slot);
                if (available == null) continue;
                if (available.amount < minimumStock.getAsInt()) continue;

                available.amount = Math.min(available.amount, maxDrain);
                if (doDrain) return extract(slot, current, available);
                return available;
            }
            return null;
        }

        private FluidStack extract(int slot, FluidSnapshot current, FluidStack request) {
            FluidStack extracted = extractor.extract(slot, request.copy());
            if (extracted == null || !extracted.isFluidEqual(request) || extracted.amount <= 0) return null;

            extracted.amount = Math.min(extracted.amount, request.amount);
            publisher.accept(current.withoutDrainedAmount(slot, request, extracted.amount));
            return extracted;
        }
    }
}
