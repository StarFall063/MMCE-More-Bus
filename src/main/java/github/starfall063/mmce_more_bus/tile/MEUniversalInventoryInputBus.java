package github.starfall063.mmce_more_bus.tile;

import appeng.api.AEApi;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import com.mekeng.github.common.me.storage.impl.GasStorageChannel;
import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.starfall063.mmce_more_bus.MMCEMoreBus;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public final class MEUniversalInventoryInputBus extends AbstractMEInputBus
        implements MachineCombinationComponent, MachineComponentDropState {
    public static final int SLOT_COUNT = 16;
    private static final boolean DEBUG_LOGGING = Boolean.getBoolean("mmce_more_bus.debug.me");
    private static final String KEY_MARKERS = "sfc_me_inventory_markers";
    private static final String KEY_PREVIEW = "sfc_me_inventory_preview";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_STACK = "Stack";
    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_GAS = "Gas";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_MIN_STACK_SIZE = "MinStackSize";

    private final Marker[] markers = new Marker[SLOT_COUNT];
    private final int[] previewAmounts = new int[SLOT_COUNT];
    private final IItemHandlerModifiable itemHandler;
    private final IFluidHandler fluidHandler;
    private final IExtendedGasHandler gasHandler;
    private final ItemExtractor itemExtractor;
    private final FluidExtractor fluidExtractor;
    private final GasExtractor gasExtractor;
    private final InfItemFluidHandler combinedHandler = new CombinedHandler();
    private int minimumStock = 1;

    public MEUniversalInventoryInputBus() {
        this(null, null, null);
    }

    MEUniversalInventoryInputBus(ItemExtractor itemExtractor, FluidExtractor fluidExtractor, GasExtractor gasExtractor) {
        this.itemExtractor = itemExtractor == null ? this::extractItemFromNetwork : itemExtractor;
        this.fluidExtractor = fluidExtractor == null ? this::extractFluidFromNetwork : fluidExtractor;
        this.gasExtractor = gasExtractor == null ? this::extractGasFromNetwork : gasExtractor;
        itemHandler = new VirtualItemHandler();
        fluidHandler = new VirtualFluidHandler();
        gasHandler = new VirtualGasHandler();
    }

    private static int clampAmount(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, amount));
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Universal inventory marker slot out of range: " + slot);
        }
    }

    private static boolean matchesItem(ItemStack extracted, ItemStack request) {
        return extracted != null && !extracted.isEmpty()
                && ItemStack.areItemsEqual(extracted, request)
                && ItemStack.areItemStackTagsEqual(extracted, request);
    }

    private static String describe(ItemStack stack) {
        if (stack == null) return "null";
        if (stack.isEmpty()) return "empty";
        return stack.getItem().getRegistryName() + "x" + stack.getCount();
    }

    @Override
    public void validate() {
        super.validate();
        debug("validated hasMarkers=" + hasMarkers());
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.meItemInputBus);
    }

    @Override
    public MachineComponent.ItemBus provideComponent() {
        debug("provideComponent item input");
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public long getGroupID() {
                return MEUniversalInventoryInputBus.this.getGroupId();
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                debug("getContainerProvider item input");
                return combinedHandler;
            }
        };
    }

    @Override
    public Collection<MachineComponent<?>> provideComponents() {
        return Collections.emptyList();
    }

    public boolean setItemMarker(int slot, ItemStack stack) {
        checkSlot(slot);
        if (stack.isEmpty()) return false;

        if (!setMarker(slot, Marker.item(stack))) return false;
        return true;
    }

    public boolean setFluidMarker(int slot, FluidStack stack) {
        checkSlot(slot);
        if (stack == null || stack.getFluid() == null) return false;

        return setMarker(slot, Marker.fluid(stack));
    }

    public boolean setGasMarker(int slot, GasStack stack) {
        checkSlot(slot);
        if (stack == null || stack.getGas() == null) return false;

        return setMarker(slot, Marker.gas(stack));
    }

    public void clearMarker(int slot) {
        checkSlot(slot);
        if (markers[slot] == null) return;
        markers[slot] = null;
        previewAmounts[slot] = 0;
        invalidateMarkers();
    }

    public MarkerType getMarkerType(int slot) {
        checkSlot(slot);
        return markers[slot] == null ? MarkerType.EMPTY : markers[slot].type;
    }

    public void publishItemAmount(int slot, long amount) {
        checkSlot(slot);
        if (getMarkerType(slot) != MarkerType.ITEM) return;
        previewAmounts[slot] = clampAmount(amount);
    }

    public void publishFluidAmount(int slot, long amount) {
        checkSlot(slot);
        if (getMarkerType(slot) != MarkerType.FLUID) return;
        previewAmounts[slot] = clampAmount(amount);
    }

    public void publishGasAmount(int slot, long amount) {
        checkSlot(slot);
        if (getMarkerType(slot) != MarkerType.GAS) return;
        previewAmounts[slot] = clampAmount(amount);
    }

    public int getPreviewAmount(int slot) {
        checkSlot(slot);
        return previewAmounts[slot];
    }

    public int getMinStackSize() {
        return minimumStock;
    }

    public void setMinStackSize(int value) {
        int replacement = Math.max(1, value);
        if (minimumStock == replacement) return;
        minimumStock = replacement;
        invalidateMarkers();
    }

    public IItemHandlerModifiable getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public IExtendedGasHandler getGasHandler() {
        return gasHandler;
    }

    public ItemStack getItemMarker(int slot) {
        checkSlot(slot);
        return markers[slot] == null || markers[slot].type != MarkerType.ITEM
                ? ItemStack.EMPTY
                : markers[slot].item.copy();
    }

    public FluidStack getFluidMarker(int slot) {
        checkSlot(slot);
        return markers[slot] == null || markers[slot].type != MarkerType.FLUID
                ? null
                : markers[slot].fluid.copy();
    }

    public GasStack getGasMarker(int slot) {
        checkSlot(slot);
        return markers[slot] == null || markers[slot].type != MarkerType.GAS
                ? null
                : markers[slot].gas.copy();
    }

    public void writeMarkerState(NBTTagCompound compound) {
        compound.setTag(KEY_MARKERS, writeMarkerEntries());
    }

    public void readMarkerState(NBTTagCompound compound) {
        clearMarkers();
        readMarkerEntries(compound.getTagList(KEY_MARKERS, 10), false);
    }

    public void writePreviewState(NBTTagCompound compound) {
        NBTTagCompound preview = new NBTTagCompound();
        preview.setTag(KEY_MARKERS, writeMarkerEntries());
        preview.setInteger(KEY_MIN_STACK_SIZE, minimumStock);

        NBTTagList amounts = new NBTTagList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (markers[slot] == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte(KEY_SLOT, (byte) slot);
            entry.setInteger(KEY_AMOUNT, previewAmounts[slot]);
            amounts.appendTag(entry);
        }
        preview.setTag(KEY_PREVIEW, amounts);
        compound.setTag(KEY_PREVIEW, preview);
    }

    public void readPreviewState(NBTTagCompound compound) {
        if (!compound.hasKey(KEY_PREVIEW, 10)) return;
        NBTTagCompound preview = compound.getCompoundTag(KEY_PREVIEW);
        clearMarkers();
        minimumStock = Math.max(1, preview.getInteger(KEY_MIN_STACK_SIZE));
        readMarkerEntries(preview.getTagList(KEY_MARKERS, 10), false);
        for (int index = 0; index < preview.getTagList(KEY_PREVIEW, 10).tagCount(); index++) {
            NBTTagCompound entry = preview.getTagList(KEY_PREVIEW, 10).getCompoundTagAt(index);
            int slot = entry.getByte(KEY_SLOT) & 0xFF;
            if (slot < SLOT_COUNT && markers[slot] != null) {
                previewAmounts[slot] = clampAmount(entry.getInteger(KEY_AMOUNT));
            }
        }
    }

    private NBTTagList writeMarkerEntries() {
        NBTTagList entries = new NBTTagList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            Marker marker = markers[slot];
            if (marker == null) continue;

            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte(KEY_SLOT, (byte) slot);
            entry.setByte(KEY_TYPE, (byte) marker.type.ordinal());
            if (marker.type == MarkerType.ITEM) {
                entry.setTag(KEY_STACK, marker.item.writeToNBT(new NBTTagCompound()));
            } else if (marker.type == MarkerType.FLUID) {
                entry.setTag(KEY_FLUID, marker.fluid.writeToNBT(new NBTTagCompound()));
            } else if (marker.type == MarkerType.GAS) {
                entry.setTag(KEY_GAS, marker.gas.write(new NBTTagCompound()));
            }
            entries.appendTag(entry);
        }
        return entries;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readDropState(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        writeDropState(compound);
    }

    @Override
    public void readDropState(NBTTagCompound compound) {
        readMarkerState(compound);
        minimumStock = Math.max(1, compound.getInteger(KEY_MIN_STACK_SIZE));
    }

    @Override
    public void writeDropState(NBTTagCompound compound) {
        writeMarkerState(compound);
        compound.setInteger(KEY_MIN_STACK_SIZE, minimumStock);
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

    private void readMarkerEntries(NBTTagList entries, boolean restoreAmounts) {
        for (int index = 0; index < entries.tagCount(); index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            int slot = entry.getByte(KEY_SLOT) & 0xFF;
            int type = entry.getByte(KEY_TYPE) & 0xFF;
            if (slot >= SLOT_COUNT || type <= MarkerType.EMPTY.ordinal() || type > MarkerType.GAS.ordinal()) continue;

            Marker marker = null;
            if (type == MarkerType.ITEM.ordinal() && entry.hasKey(KEY_STACK, 10)) {
                ItemStack stack = new ItemStack(entry.getCompoundTag(KEY_STACK));
                if (!stack.isEmpty()) marker = Marker.item(stack);
            } else if (type == MarkerType.FLUID.ordinal() && entry.hasKey(KEY_FLUID, 10)) {
                marker = Marker.fluid(FluidStack.loadFluidStackFromNBT(entry.getCompoundTag(KEY_FLUID)));
            } else if (type == MarkerType.GAS.ordinal() && entry.hasKey(KEY_GAS, 10)) {
                marker = Marker.gas(GasStack.readFromNBT(entry.getCompoundTag(KEY_GAS)));
            }
            if (marker == null || containsMarkerInOtherSlot(slot, marker)) continue;

            markers[slot] = marker;
            if (restoreAmounts) previewAmounts[slot] = clampAmount(entry.getInteger(KEY_AMOUNT));
        }
    }

    private boolean containsMarkerInOtherSlot(int excludedSlot, Marker candidate) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (slot == excludedSlot || markers[slot] == null) continue;
            if (markers[slot].sameIdentity(candidate)) return true;
        }
        return false;
    }

    private void clearMarkers() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            markers[slot] = null;
            previewAmounts[slot] = 0;
        }
    }

    private ItemStack extractItemFromNetwork(int slot, ItemStack request) {
        debug("extractItemFromNetwork slot=" + slot + " request=" + describe(request));
        try {
            IStorageGrid storageGrid = getProxy().getStorage();
            if (storageGrid == null) {
                debug("extractItemFromNetwork no storage grid");
                return ItemStack.EMPTY;
            }
            IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> monitor = storageGrid.getInventory(channel);
            if (monitor == null) {
                debug("extractItemFromNetwork no item monitor");
                return ItemStack.EMPTY;
            }
            IAEItemStack aeRequest = channel.createStack(request);
            if (aeRequest == null) {
                debug("extractItemFromNetwork no AE request");
                return ItemStack.EMPTY;
            }
            IAEItemStack extracted = appeng.util.Platform.poweredExtraction(getProxy().getEnergy(), monitor, aeRequest, source);
            ItemStack result = extracted == null ? ItemStack.EMPTY : extracted.createItemStack();
            debug("extractItemFromNetwork result=" + describe(result));
            return result;
        } catch (GridAccessException ignored) {
            debug("extractItemFromNetwork grid access failure");
            return ItemStack.EMPTY;
        }
    }

    private FluidStack extractFluidFromNetwork(int slot, FluidStack request) {
        try {
            IStorageGrid storageGrid = getProxy().getStorage();
            if (storageGrid == null) return null;
            IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
            IMEMonitor<IAEFluidStack> monitor = storageGrid.getInventory(channel);
            if (monitor == null) return null;
            IAEFluidStack aeRequest = channel.createStack(request);
            if (aeRequest == null) return null;
            IAEFluidStack extracted = appeng.util.Platform.poweredExtraction(getProxy().getEnergy(), monitor, aeRequest, source);
            return extracted == null ? null : extracted.getFluidStack();
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    private GasStack extractGasFromNetwork(int slot, GasStack request) {
        try {
            IStorageGrid storageGrid = getProxy().getStorage();
            if (storageGrid == null) return null;
            IGasStorageChannel channel = GasStorageChannel.INSTANCE;
            IMEMonitor<IAEGasStack> monitor = storageGrid.getInventory(channel);
            if (monitor == null) return null;
            IAEGasStack aeRequest = AEGasStack.of(request);
            if (aeRequest == null) return null;
            IAEGasStack extracted = appeng.util.Platform.poweredExtraction(getProxy().getEnergy(), monitor, aeRequest, source);
            return extracted == null ? null : extracted.getGasStack();
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    private boolean setMarker(int slot, Marker marker) {
        if (marker == null || containsMarkerInOtherSlot(slot, marker)) return false;
        markers[slot] = marker;
        previewAmounts[slot] = 0;
        debug("setMarker slot=" + slot + " type=" + marker.type);
        invalidateMarkers();
        return true;
    }

    public boolean hasMarkers() {
        for (Marker marker : markers) {
            if (marker != null) return true;
        }
        return false;
    }

    public boolean hasDropConfiguration() {
        return hasMarkers() || minimumStock != 1;
    }

    @Override
    protected boolean hasActiveConfiguration() {
        return hasMarkers();
    }

    private void invalidateMarkers() {
        if (getWorld() == null) return;
        markDirty();
        alertTickingDevice();
        markForUpdateSync();
    }

    @Override
    protected void refreshSnapshot() {
        refreshSnapshots();
    }

    private void refreshSnapshots() {
        int[] previousAmounts = previewAmounts.clone();
        clearPreviewAmounts();
        if (!getProxy().isActive()) {
            synchronizePreview(previousAmounts);
            return;
        }

        try {
            IStorageGrid storageGrid = getProxy().getStorage();
            if (storageGrid == null) {
                synchronizePreview(previousAmounts);
                return;
            }
            refreshItemSnapshot(storageGrid);
            refreshFluidSnapshot(storageGrid);
            refreshGasSnapshot(storageGrid);
        } catch (GridAccessException ignored) {
            // A disconnected grid publishes zero availability until the next tick.
        }
        synchronizePreview(previousAmounts);
    }

    private void refreshItemSnapshot(IStorageGrid storageGrid) {
        IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IMEMonitor<IAEItemStack> monitor = storageGrid.getInventory(channel);
        if (monitor == null) return;
        IItemList<IAEItemStack> stored = monitor.getStorageList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            Marker marker = markers[slot];
            if (marker == null || marker.type != MarkerType.ITEM) continue;

            IAEItemStack request = channel.createStack(marker.item);
            IAEItemStack entry = request == null ? null : stored.findPrecise(request);
            previewAmounts[slot] = entry == null ? 0 : clampAmount(entry.getStackSize());
        }
    }

    private void refreshFluidSnapshot(IStorageGrid storageGrid) {
        IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        IMEMonitor<IAEFluidStack> monitor = storageGrid.getInventory(channel);
        if (monitor == null) return;
        IItemList<IAEFluidStack> stored = monitor.getStorageList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            Marker marker = markers[slot];
            if (marker == null || marker.type != MarkerType.FLUID) continue;

            IAEFluidStack request = channel.createStack(marker.fluid);
            IAEFluidStack entry = request == null ? null : stored.findPrecise(request);
            previewAmounts[slot] = entry == null ? 0 : clampAmount(entry.getStackSize());
        }
    }

    private void refreshGasSnapshot(IStorageGrid storageGrid) {
        IGasStorageChannel channel = GasStorageChannel.INSTANCE;
        IMEMonitor<IAEGasStack> monitor = storageGrid.getInventory(channel);
        if (monitor == null) return;
        IItemList<IAEGasStack> stored = monitor.getStorageList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            Marker marker = markers[slot];
            if (marker == null || marker.type != MarkerType.GAS) continue;

            IAEGasStack request = channel.createStack(marker.gas);
            IAEGasStack entry = request == null ? null : stored.findPrecise(request);
            previewAmounts[slot] = entry == null ? 0 : clampAmount(entry.getStackSize());
        }
    }

    private void clearPreviewAmounts() {
        Arrays.fill(previewAmounts, 0);
    }

    private void synchronizePreview(int[] previousAmounts) {
        if (getWorld() != null && !Arrays.equals(previousAmounts, previewAmounts)) markForUpdateSync();
    }

    private FluidStack getFluidStack(int slot) {
        checkSlot(slot);
        Marker marker = markers[slot];
        if (marker == null || marker.type != MarkerType.FLUID || previewAmounts[slot] <= 0) return null;
        FluidStack stack = marker.fluid.copy();
        stack.amount = previewAmounts[slot];
        return stack;
    }

    private GasStack getGasStack(int slot) {
        checkSlot(slot);
        Marker marker = markers[slot];
        if (marker == null || marker.type != MarkerType.GAS || previewAmounts[slot] <= 0) return null;
        GasStack stack = marker.gas.copy();
        stack.amount = previewAmounts[slot];
        return stack;
    }

    private boolean hasMarkerType(MarkerType type) {
        for (Marker marker : markers) {
            if (marker != null && marker.type == type) return true;
        }
        return false;
    }

    private void debug(String message) {
        if (DEBUG_LOGGING) MMCEMoreBus.LOGGER.debug("[DEBUG-universal-input] pos={} {}", getPos(), message);
    }

    public enum MarkerType {
        EMPTY,
        ITEM,
        FLUID,
        GAS
    }

    @FunctionalInterface
    interface ItemExtractor {
        ItemStack extract(int slot, ItemStack request);
    }

    @FunctionalInterface
    interface FluidExtractor {
        FluidStack extract(int slot, FluidStack request);
    }

    @FunctionalInterface
    interface GasExtractor {
        GasStack extract(int slot, GasStack request);
    }

    private static final class Marker {
        private final MarkerType type;
        private final ItemStack item;
        private final FluidStack fluid;
        private final GasStack gas;

        private Marker(MarkerType type, ItemStack item, FluidStack fluid, GasStack gas) {
            this.type = type;
            this.item = item;
            this.fluid = fluid;
            this.gas = gas;
        }

        private static Marker item(ItemStack stack) {
            ItemStack identity = stack.copy();
            identity.setCount(1);
            return new Marker(MarkerType.ITEM, identity, null, null);
        }

        private static Marker fluid(FluidStack stack) {
            if (stack == null || stack.getFluid() == null) return null;
            FluidStack identity = stack.copy();
            identity.amount = 1;
            return new Marker(MarkerType.FLUID, null, identity, null);
        }

        private static Marker gas(GasStack stack) {
            if (stack == null || stack.getGas() == null) return null;
            GasStack identity = stack.copy();
            identity.amount = 1;
            return new Marker(MarkerType.GAS, null, null, identity);
        }

        private boolean sameIdentity(Marker other) {
            if (type != other.type) return false;
            if (type == MarkerType.ITEM) {
                return ItemStack.areItemsEqual(item, other.item)
                        && ItemStack.areItemStackTagsEqual(item, other.item);
            }
            if (type == MarkerType.FLUID) return fluid.isFluidEqual(other.fluid);
            if (type == MarkerType.GAS) return gas.isGasEqual(other.gas);
            return false;
        }
    }

    private final class CombinedHandler extends InfItemFluidHandler {
        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return fluidHandler.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return fluidHandler.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return fluidHandler.drain(resource, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return fluidHandler.drain(maxDrain, doDrain);
        }

        @Override
        public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
            return gasHandler.receiveGas(side, stack, doTransfer);
        }

        @Override
        public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
            return gasHandler.drawGas(side, amount, doTransfer);
        }

        @Override
        public GasStack drawGas(GasStack request, boolean doTransfer) {
            return gasHandler.drawGas(request, doTransfer);
        }

        @Override
        public boolean canReceiveGas(EnumFacing side, Gas gas) {
            return gasHandler.canReceiveGas(side, gas);
        }

        @Override
        public boolean canDrawGas(EnumFacing side, Gas gas) {
            return gasHandler.canDrawGas(side, gas);
        }

        @Override
        public mekanism.api.gas.GasTankInfo[] getTankInfo() {
            return gasHandler.getTankInfo();
        }
    }

    private final class VirtualItemHandler implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            checkSlot(slot);
            Marker marker = markers[slot];
            if (marker == null || marker.type != MarkerType.ITEM || previewAmounts[slot] < minimumStock) {
                if (marker != null && marker.type == MarkerType.ITEM) {
                    debug("getStackInSlot slot=" + slot + " hidden amount=" + previewAmounts[slot]
                            + " minimum=" + minimumStock);
                }
                return ItemStack.EMPTY;
            }
            ItemStack stack = marker.item.copy();
            stack.setCount(previewAmounts[slot]);
            debug("getStackInSlot slot=" + slot + " exposed=" + describe(stack));
            return stack;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            checkSlot(slot);
            if (amount <= 0) return ItemStack.EMPTY;

            ItemStack available = getStackInSlot(slot);
            if (available.isEmpty()) return ItemStack.EMPTY;
            ItemStack request = available.copy();
            request.setCount(Math.min(amount, available.getCount()));
            debug("extractItem slot=" + slot + " simulate=" + simulate + " request=" + describe(request));
            if (simulate) return request;

            ItemStack extracted = itemExtractor.extract(slot, request.copy());
            if (!matchesItem(extracted, request)) {
                debug("extractItem rejected result=" + describe(extracted));
                return ItemStack.EMPTY;
            }
            extracted.setCount(Math.min(extracted.getCount(), request.getCount()));
            previewAmounts[slot] -= extracted.getCount();
            debug("extractItem success result=" + describe(extracted)
                    + " remainingPreview=" + previewAmounts[slot]);
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            checkSlot(slot);
            ItemStack available = getStackInSlot(slot);
            debug("setStackInSlot slot=" + slot + " current=" + describe(available)
                    + " replacement=" + describe(stack));
            if (available.isEmpty()) return;
            if (!stack.isEmpty() && !matchesItem(available, stack)) return;

            int remaining = stack.isEmpty() ? 0 : Math.min(available.getCount(), Math.max(0, stack.getCount()));
            int consumed = available.getCount() - remaining;
            if (consumed > 0) extractItem(slot, consumed, false);
        }
    }

    private final class VirtualFluidHandler implements IFluidHandler {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] properties = new IFluidTankProperties[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                properties[slot] = new net.minecraftforge.fluids.capability.FluidTankProperties(
                        getFluidStack(slot), Integer.MAX_VALUE, false, true
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
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                FluidStack available = getFluidStack(slot);
                if (available == null || !available.isFluidEqual(resource)) continue;
                if (available.amount < minimumStock) return null;
                FluidStack request = available.copy();
                request.amount = Math.min(request.amount, resource.amount);
                return doDrain ? drainFromNetwork(slot, request) : request;
            }
            return null;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0) return null;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                FluidStack available = getFluidStack(slot);
                if (available == null || available.amount < minimumStock) continue;
                available.amount = Math.min(available.amount, maxDrain);
                return doDrain ? drainFromNetwork(slot, available) : available;
            }
            return null;
        }

        private FluidStack drainFromNetwork(int slot, FluidStack request) {
            FluidStack extracted = fluidExtractor.extract(slot, request.copy());
            if (extracted == null || !extracted.isFluidEqual(request) || extracted.amount <= 0) return null;
            extracted.amount = Math.min(extracted.amount, request.amount);
            previewAmounts[slot] -= extracted.amount;
            return extracted;
        }
    }

    private final class VirtualGasHandler implements IExtendedGasHandler {
        @Override
        public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
            return 0;
        }

        @Override
        public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
            if (amount <= 0) return null;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack available = getGasStack(slot);
                if (available == null || available.amount < minimumStock) continue;
                available.amount = Math.min(available.amount, amount);
                return doTransfer ? drawFromNetwork(slot, available) : available;
            }
            return null;
        }

        @Override
        public GasStack drawGas(GasStack request, boolean doTransfer) {
            if (request == null || request.getGas() == null || request.amount <= 0) return null;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack available = getGasStack(slot);
                if (available == null || !available.isGasEqual(request)) continue;
                if (available.amount < minimumStock) return null;
                available.amount = Math.min(available.amount, request.amount);
                return doTransfer ? drawFromNetwork(slot, available) : available;
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
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack available = getGasStack(slot);
                if (available != null && available.getGas() == gas) return true;
            }
            return false;
        }

        @Override
        public GasTankInfo[] getTankInfo() {
            GasTankInfo[] tanks = new GasTankInfo[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                GasStack gas = getGasStack(slot);
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

        private GasStack drawFromNetwork(int slot, GasStack request) {
            GasStack extracted = gasExtractor.extract(slot, request.copy());
            if (extracted == null || !extracted.isGasEqual(request) || extracted.amount <= 0) return null;
            extracted.amount = Math.min(extracted.amount, request.amount);
            previewAmounts[slot] -= extracted.amount;
            return extracted;
        }
    }
}
