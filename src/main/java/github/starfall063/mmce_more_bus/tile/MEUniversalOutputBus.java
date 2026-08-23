package github.starfall063.mmce_more_bus.tile;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.ITickManager;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import com.mekeng.github.common.me.storage.impl.GasStorageChannel;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.starfall063.mmce_more_bus.MMCEMoreBus;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.*;

public final class MEUniversalOutputBus extends MEMachineComponent implements IGridTickable, MachineCombinationComponent {
    public static final int DEFAULT_MAX_DISTINCT_RESOURCES = 128;
    private static final boolean DEBUG_LOGGING = Boolean.getBoolean("mmce_more_bus.debug.me");
    private static final int MAX_FLUSHES_PER_TICK = 16;
    private static final String KEY_BUFFER = "sfc_me_universal_output_buffer";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_IDENTITY = "Identity";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_VIEWPORT = "sfc_me_universal_output_viewport";
    private static final String KEY_ROW_OFFSET = "RowOffset";
    private static final String KEY_MAX_ROW_OFFSET = "MaxRowOffset";
    private static final String KEY_VIEWPORT_REVISION = "Revision";
    private static final int VIEWPORT_COLUMNS = 9;
    private static final int VIEWPORT_ROWS = 4;
    private static final int VIEWPORT_SIZE = 36;
    private static final GasTankInfo EMPTY_UNLIMITED_GAS_TANK = new GasTankInfo() {
        @Override
        public GasStack getGas() {
            return null;
        }

        @Override
        public int getStored() {
            return 0;
        }

        @Override
        public int getMaxGas() {
            return Integer.MAX_VALUE;
        }
    };
    private final int maxDistinctResources;
    private final Map<ResourceKey, Long> bufferedAmounts = new LinkedHashMap<>();
    private final Deque<ResourceKey> pendingResources = new ArrayDeque<>();
    private final Set<ResourceKey> queuedResources = new HashSet<>();
    private final PendingResourceInserter resourceInserter;
    private final IFluidTankProperties[] fluidOutputProperties;
    private final GasTankInfo[] gasOutputTanks;
    private final IFluidHandler fluidOutput;
    private final IExtendedGasHandler gasOutput;
    private final CombinedOutputHandler combinedOutput;
    private List<DisplayResource> clientViewport = Collections.emptyList();
    private int clientViewportRowOffset;
    private int clientViewportMaxRowOffset;
    private int clientViewportRevision = -1;
    private List<DisplayResource> sortedResources = Collections.emptyList();
    private boolean sortedResourcesDirty = true;
    private int viewportRevision;
    public MEUniversalOutputBus() {
        this(MEUniversalOutputBusConfig.maxDistinctResources(
                MEUniversalOutputBusConfig.maximumDistinctResources
        ));
    }

    MEUniversalOutputBus(int maxDistinctResources) {
        this(maxDistinctResources, null);
    }

    MEUniversalOutputBus(int maxDistinctResources, PendingResourceInserter resourceInserter) {
        this.maxDistinctResources = Math.max(1, maxDistinctResources);
        this.resourceInserter = resourceInserter == null ? this::insertIntoNetwork : resourceInserter;
        this.fluidOutputProperties = createFluidOutputProperties(this.maxDistinctResources);
        this.gasOutputTanks = createGasOutputTanks(this.maxDistinctResources);
        this.fluidOutput = new FluidOutputHandler();
        this.gasOutput = new GasOutputHandler();
        this.combinedOutput = new CombinedOutputHandler(this.maxDistinctResources);
    }

    private static int[] slotIndexes(int slotCount) {
        int[] slots = new int[slotCount];
        for (int slot = 0; slot < slotCount; slot++) slots[slot] = slot;
        return slots;
    }

    private static IFluidTankProperties[] createFluidOutputProperties(int tankCount) {
        IFluidTankProperties[] properties = new IFluidTankProperties[tankCount];
        Arrays.fill(properties, new FluidTankProperties(null, Integer.MAX_VALUE, true, false));
        return properties;
    }

    private static GasTankInfo[] createGasOutputTanks(int tankCount) {
        GasTankInfo[] tanks = new GasTankInfo[tankCount];
        Arrays.fill(tanks, EMPTY_UNLIMITED_GAS_TANK);
        return tanks;
    }

    static int maxViewportRowOffset(int resourceCount) {
        int totalRows = (Math.max(0, resourceCount) + VIEWPORT_COLUMNS - 1) / VIEWPORT_COLUMNS;
        return Math.max(0, totalRows - VIEWPORT_ROWS);
    }

    static int normalizeViewportRowOffset(int rowOffset, int maxRowOffset) {
        return Math.max(0, Math.min(rowOffset, Math.max(0, maxRowOffset)));
    }

    private static long clampRemainder(long amount, long remainder) {
        return Math.max(0L, Math.min(amount, remainder));
    }

    static boolean shouldAlertTickManager(boolean pendingWasEmpty, boolean pendingIsEmpty) {
        return pendingWasEmpty && !pendingIsEmpty;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.meItemOutputBus);
    }

    @Override
    public MachineComponent.ItemBus provideComponent() {
        debug("provideComponent item output");
        return new MachineComponent.ItemBus(IOType.OUTPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public IItemHandlerImpl getContainerProvider() {
                debug("getContainerProvider item output");
                return combinedOutput;
            }
        };
    }

    @Override
    public java.util.Collection<MachineComponent<?>> provideComponents() {
        return Collections.emptyList();
    }

    public ItemStack offerItem(ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            debug("offerItem rejected empty simulate=" + simulate);
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }
        ResourceKey key = ResourceKey.item(stack);
        if (!canAccept(key)) {
            debug("offerItem rejected capacity simulate=" + simulate + " amount=" + stack.getCount()
                    + " distinct=" + bufferedAmounts.size() + "/" + maxDistinctResources);
            return stack.copy();
        }
        debug("offerItem accepted simulate=" + simulate + " amount=" + stack.getCount());
        if (!simulate) {
            boolean pendingWasEmpty = pendingResources.isEmpty();
            add(key, stack.getCount());
            onBufferChanged(shouldAlertTickManager(pendingWasEmpty, pendingResources.isEmpty()));
        }
        return ItemStack.EMPTY;
    }

    public int offerFluid(FluidStack stack, boolean simulate) {
        if (stack == null || stack.getFluid() == null || stack.amount <= 0) return 0;
        ResourceKey key = ResourceKey.fluid(stack);
        if (!canAccept(key)) {
            debug("offerFluid rejected capacity simulate=" + simulate + " amount=" + stack.amount
                    + " distinct=" + bufferedAmounts.size() + "/" + maxDistinctResources);
            return 0;
        }
        debug("offerFluid accepted simulate=" + simulate + " amount=" + stack.amount);
        if (!simulate) {
            boolean pendingWasEmpty = pendingResources.isEmpty();
            add(key, stack.amount);
            onBufferChanged(shouldAlertTickManager(pendingWasEmpty, pendingResources.isEmpty()));
        }
        return stack.amount;
    }

    public int offerGas(GasStack stack, boolean simulate) {
        if (stack == null || stack.getGas() == null || stack.amount <= 0) return 0;
        ResourceKey key = ResourceKey.gas(stack);
        if (!canAccept(key)) {
            debug("offerGas rejected capacity simulate=" + simulate + " amount=" + stack.amount
                    + " distinct=" + bufferedAmounts.size() + "/" + maxDistinctResources);
            return 0;
        }
        debug("offerGas accepted simulate=" + simulate + " amount=" + stack.amount);
        if (!simulate) {
            boolean pendingWasEmpty = pendingResources.isEmpty();
            add(key, stack.amount);
            onBufferChanged(shouldAlertTickManager(pendingWasEmpty, pendingResources.isEmpty()));
        }
        return stack.amount;
    }

    public int getDistinctResourceCount() {
        return bufferedAmounts.size();
    }

    public long getBufferedItemAmount(ItemStack stack) {
        return stack.isEmpty() ? 0L : bufferedAmounts.getOrDefault(ResourceKey.item(stack), 0L);
    }

    public long getBufferedFluidAmount(FluidStack stack) {
        return stack == null || stack.getFluid() == null ? 0L : bufferedAmounts.getOrDefault(ResourceKey.fluid(stack), 0L);
    }

    public long getBufferedGasAmount(GasStack stack) {
        return stack == null || stack.getGas() == null ? 0L : bufferedAmounts.getOrDefault(ResourceKey.gas(stack), 0L);
    }

    public List<DisplayResource> createViewport(int rowOffset) {
        List<DisplayResource> entries = sortedResources();
        int start = Math.max(0, rowOffset) * VIEWPORT_COLUMNS;
        if (start >= entries.size()) return java.util.Collections.emptyList();

        int end = Math.min(entries.size(), start + VIEWPORT_SIZE);
        return new ArrayList<>(entries.subList(start, end));
    }

    public NBTTagCompound createViewportState(int rowOffset) {
        return createViewportState(rowOffset, -1, -1);
    }

    public NBTTagCompound createViewportState(int rowOffset, int knownRevision, int knownRowOffset) {
        NBTTagCompound state = new NBTTagCompound();
        int maxRowOffset = maxViewportRowOffset(bufferedAmounts.size());
        int normalizedRowOffset = normalizeViewportRowOffset(rowOffset, maxRowOffset);
        state.setInteger(KEY_VIEWPORT_REVISION, viewportRevision);
        state.setInteger(KEY_ROW_OFFSET, normalizedRowOffset);
        state.setInteger(KEY_MAX_ROW_OFFSET, maxRowOffset);
        if (knownRevision == viewportRevision && knownRowOffset == normalizedRowOffset) return state;

        NBTTagList entries = new NBTTagList();
        for (DisplayResource resource : createViewport(normalizedRowOffset)) {
            entries.appendTag(resource.key.write(resource.amount));
        }
        state.setTag(KEY_VIEWPORT, entries);
        return state;
    }

    public void readViewportState(NBTTagCompound state) {
        clientViewportMaxRowOffset = Math.max(0, state.getInteger(KEY_MAX_ROW_OFFSET));
        clientViewportRowOffset = normalizeViewportRowOffset(
                state.getInteger(KEY_ROW_OFFSET),
                clientViewportMaxRowOffset
        );
        clientViewportRevision = state.getInteger(KEY_VIEWPORT_REVISION);
        if (!state.hasKey(KEY_VIEWPORT, 9)) return;

        List<DisplayResource> entries = new ArrayList<>();
        NBTTagList stored = state.getTagList(KEY_VIEWPORT, 10);
        for (int index = 0; index < stored.tagCount(); index++) {
            NBTTagCompound entry = stored.getCompoundTagAt(index);
            ResourceKey key = ResourceKey.read(entry);
            long amount = entry.getLong(KEY_AMOUNT);
            if (key != null && amount > 0L) entries.add(new DisplayResource(key, amount));
        }
        clientViewport = Collections.unmodifiableList(entries);
    }

    public List<DisplayResource> getClientViewport() {
        return clientViewport;
    }

    public int getClientViewportRowOffset() {
        return clientViewportRowOffset;
    }

    public int getClientViewportMaxRowOffset() {
        return clientViewportMaxRowOffset;
    }

    public int getClientViewportRevision() {
        return clientViewportRevision;
    }

    public int getViewportRevision() {
        return viewportRevision;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 60, pendingResources.isEmpty(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!getProxy().isActive()) return TickRateModulation.IDLE;
        return flushPending() ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    boolean flushPending() {
        debug("flushPending start pending=" + pendingResources.size() + " buffered=" + bufferedAmounts.size());
        boolean changed = false;
        int attempts = Math.min(MAX_FLUSHES_PER_TICK, pendingResources.size());
        for (int index = 0; index < attempts; index++) {
            ResourceKey key = pendingResources.pollFirst();
            Long amount = bufferedAmounts.get(key);
            if (amount == null || amount <= 0L) {
                queuedResources.remove(key);
                continue;
            }

            long insertedRemainder = resourceInserter.insert(new BufferedResource(key, amount));
            long remainder = clampRemainder(amount, insertedRemainder);
            debug("flushPending resource type=" + key.type + " requested=" + amount
                    + " inserterRemainder=" + insertedRemainder + " clampedRemainder=" + remainder);
            if (remainder == 0L) {
                bufferedAmounts.remove(key);
                queuedResources.remove(key);
                changed = true;
            } else {
                if (remainder != amount) changed = true;
                bufferedAmounts.put(key, remainder);
                pendingResources.addLast(key);
            }
        }
        if (changed) {
            invalidateViewportCache();
            onBufferChanged(false);
        }
        return changed;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readBufferState(compound);
    }

    void readBufferState(NBTTagCompound compound) {
        bufferedAmounts.clear();
        pendingResources.clear();
        queuedResources.clear();

        NBTTagList entries = compound.getTagList(KEY_BUFFER, 10);
        for (int index = 0; index < entries.tagCount(); index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            ResourceKey key = ResourceKey.read(entry);
            long amount = entry.getLong(KEY_AMOUNT);
            if (key == null || amount <= 0L) continue;
            add(key, amount);
        }
        invalidateViewportCache();
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        writeBufferState(compound);
    }

    void writeBufferState(NBTTagCompound compound) {
        NBTTagList entries = new NBTTagList();
        for (Map.Entry<ResourceKey, Long> entry : bufferedAmounts.entrySet()) {
            NBTTagCompound stored = entry.getKey().write(entry.getValue());
            entries.appendTag(stored);
        }
        compound.setTag(KEY_BUFFER, entries);
    }

    private boolean canAccept(ResourceKey key) {
        return bufferedAmounts.containsKey(key) || bufferedAmounts.size() < maxDistinctResources;
    }

    private void add(ResourceKey key, long amount) {
        long current = bufferedAmounts.getOrDefault(key, 0L);
        long replacement = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        bufferedAmounts.put(key, replacement);
        if (queuedResources.add(key)) pendingResources.addLast(key);
        invalidateViewportCache();
    }

    private List<DisplayResource> sortedResources() {
        if (!sortedResourcesDirty) return sortedResources;

        List<Map.Entry<ResourceKey, Long>> entries = new ArrayList<>(bufferedAmounts.entrySet());
        entries.sort(Comparator.comparing(Map.Entry<ResourceKey, Long>::getKey));
        List<DisplayResource> replacement = new ArrayList<>(entries.size());
        for (Map.Entry<ResourceKey, Long> entry : entries) {
            replacement.add(new DisplayResource(entry.getKey(), entry.getValue()));
        }
        sortedResources = Collections.unmodifiableList(replacement);
        sortedResourcesDirty = false;
        return sortedResources;
    }

    private void invalidateViewportCache() {
        sortedResourcesDirty = true;
        viewportRevision = viewportRevision == Integer.MAX_VALUE ? 0 : viewportRevision + 1;
    }

    private long insertIntoNetwork(BufferedResource entry) {
        debug("insertIntoNetwork type=" + entry.key.type + " amount=" + entry.amount);
        try {
            IStorageGrid storage = getProxy().getStorage();
            if (storage == null) {
                debug("insertIntoNetwork no storage grid");
                return entry.amount;
            }
            if (entry.key.type == ResourceType.ITEM) return insertItem(storage, entry);
            if (entry.key.type == ResourceType.FLUID) return insertFluid(storage, entry);
            return insertGas(storage, entry);
        } catch (GridAccessException ignored) {
            debug("insertIntoNetwork grid access failure");
            return entry.amount;
        }
    }

    private long insertItem(IStorageGrid storage, BufferedResource entry) throws GridAccessException {
        IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IMEMonitor<IAEItemStack> monitor = storage.getInventory(channel);
        IAEItemStack stack = channel.createStack(entry.key.item);
        if (monitor == null || stack == null) return entry.amount;
        stack.setStackSize(entry.amount);
        IAEItemStack remainder = Platform.poweredInsert(getProxy().getEnergy(), monitor, stack, source);
        return remainder == null ? 0L : remainder.getStackSize();
    }

    private long insertFluid(IStorageGrid storage, BufferedResource entry) throws GridAccessException {
        IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        IMEMonitor<IAEFluidStack> monitor = storage.getInventory(channel);
        IAEFluidStack stack = channel.createStack(entry.key.fluid);
        if (monitor == null || stack == null) return entry.amount;
        stack.setStackSize(entry.amount);
        IAEFluidStack remainder = Platform.poweredInsert(getProxy().getEnergy(), monitor, stack, source);
        return remainder == null ? 0L : remainder.getStackSize();
    }

    private long insertGas(IStorageGrid storage, BufferedResource entry) throws GridAccessException {
        IGasStorageChannel channel = GasStorageChannel.INSTANCE;
        IMEMonitor<IAEGasStack> monitor = storage.getInventory(channel);
        IAEGasStack stack = AEGasStack.of(entry.key.gas);
        if (monitor == null || stack == null) return entry.amount;
        stack.setStackSize(entry.amount);
        IAEGasStack remainder = Platform.poweredInsert(getProxy().getEnergy(), monitor, stack, source);
        return remainder == null ? 0L : remainder.getStackSize();
    }

    private void onBufferChanged(boolean alertTickManager) {
        if (getWorld() == null) return;
        markDirty();
        if (!alertTickManager) return;
        try {
            ITickManager tickManager = getProxy().getTick();
            tickManager.alertDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
            // A detached grid will poll again after it reconnects.
        }
    }

    private void debug(String message) {
        if (DEBUG_LOGGING) MMCEMoreBus.LOGGER.debug("[DEBUG-universal-output] pos={} {}", getPos(), message);
    }

    private enum ResourceType {
        ITEM,
        FLUID,
        GAS
    }

    @FunctionalInterface
    interface PendingResourceInserter {
        long insert(BufferedResource entry);
    }

    private static final class ResourceKey implements Comparable<ResourceKey> {
        private final ResourceType type;
        private final ItemStack item;
        private final FluidStack fluid;
        private final GasStack gas;

        private ResourceKey(ResourceType type, ItemStack item, FluidStack fluid, GasStack gas) {
            this.type = type;
            this.item = item;
            this.fluid = fluid;
            this.gas = gas;
        }

        private static ResourceKey item(ItemStack stack) {
            ItemStack identity = stack.copy();
            identity.setCount(1);
            return new ResourceKey(ResourceType.ITEM, identity, null, null);
        }

        private static ResourceKey fluid(FluidStack stack) {
            FluidStack identity = stack.copy();
            identity.amount = 1;
            return new ResourceKey(ResourceType.FLUID, null, identity, null);
        }

        private static ResourceKey gas(GasStack stack) {
            GasStack identity = stack.copy();
            identity.amount = 1;
            return new ResourceKey(ResourceType.GAS, null, null, identity);
        }

        private static ResourceKey read(NBTTagCompound stored) {
            int typeId = stored.getByte(KEY_TYPE) & 0xFF;
            if (typeId >= ResourceType.values().length || !stored.hasKey(KEY_IDENTITY, 10)) return null;
            NBTTagCompound identity = stored.getCompoundTag(KEY_IDENTITY);
            if (typeId == ResourceType.ITEM.ordinal()) {
                ItemStack stack = new ItemStack(identity);
                return stack.isEmpty() ? null : item(stack);
            }
            if (typeId == ResourceType.FLUID.ordinal()) {
                FluidStack stack = FluidStack.loadFluidStackFromNBT(identity);
                return stack == null || stack.getFluid() == null ? null : fluid(stack);
            }
            GasStack stack = GasStack.readFromNBT(identity);
            return stack == null || stack.getGas() == null ? null : gas(stack);
        }

        private NBTTagCompound write(long amount) {
            NBTTagCompound stored = new NBTTagCompound();
            stored.setByte(KEY_TYPE, (byte) type.ordinal());
            stored.setLong(KEY_AMOUNT, amount);
            if (type == ResourceType.ITEM) {
                stored.setTag(KEY_IDENTITY, item.writeToNBT(new NBTTagCompound()));
            } else if (type == ResourceType.FLUID) {
                stored.setTag(KEY_IDENTITY, fluid.writeToNBT(new NBTTagCompound()));
            } else {
                stored.setTag(KEY_IDENTITY, gas.write(new NBTTagCompound()));
            }
            return stored;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ResourceKey key) || type != key.type) return false;
            if (type == ResourceType.ITEM) {
                return ItemStack.areItemsEqual(item, key.item)
                        && ItemStack.areItemStackTagsEqual(item, key.item);
            }
            if (type == ResourceType.FLUID) return fluid.isFluidEqual(key.fluid);
            return gas.isGasEqual(key.gas);
        }

        @Override
        public int hashCode() {
            if (type == ResourceType.ITEM) {
                return Objects.hash(type, Item.getIdFromItem(item.getItem()), item.getMetadata(), item.getTagCompound());
            }
            if (type == ResourceType.FLUID) {
                return Objects.hash(type, fluid.getFluid().getName(), fluid.tag);
            }
            return Objects.hash(type, gas.getGas().getName());
        }

        @Override
        public int compareTo(ResourceKey other) {
            int typeOrder = Integer.compare(type.ordinal(), other.type.ordinal());
            if (typeOrder != 0) return typeOrder;
            if (type == ResourceType.ITEM) {
                int itemOrder = Integer.compare(Item.getIdFromItem(item.getItem()), Item.getIdFromItem(other.item.getItem()));
                if (itemOrder != 0) return itemOrder;
                int metadataOrder = Integer.compare(item.getMetadata(), other.item.getMetadata());
                if (metadataOrder != 0) return metadataOrder;
                return String.valueOf(item.getTagCompound()).compareTo(String.valueOf(other.item.getTagCompound()));
            }
            if (type == ResourceType.FLUID) {
                int fluidOrder = fluid.getFluid().getName().compareTo(other.fluid.getFluid().getName());
                if (fluidOrder != 0) return fluidOrder;
                return String.valueOf(fluid.tag).compareTo(String.valueOf(other.fluid.tag));
            }
            return gas.getGas().getName().compareTo(other.gas.getGas().getName());
        }
    }

    public static final class DisplayResource {
        private final ResourceKey key;
        private final long amount;

        private DisplayResource(ResourceKey key, long amount) {
            this.key = key;
            this.amount = amount;
        }

        public long getAmount() {
            return amount;
        }

        public ItemStack getItem() {
            return key.type == ResourceType.ITEM ? key.item.copy() : ItemStack.EMPTY;
        }

        public FluidStack getFluid() {
            return key.type == ResourceType.FLUID ? key.fluid.copy() : null;
        }

        public GasStack getGas() {
            return key.type == ResourceType.GAS ? key.gas.copy() : null;
        }
    }

    static final class BufferedResource {
        private final ResourceKey key;
        private final long amount;

        private BufferedResource(ResourceKey key, long amount) {
            this.key = key;
            this.amount = amount;
        }

        long getAmount() {
            return amount;
        }
    }

    /**
     * MMCE copies item handlers through IItemHandlerImpl.copy() while checking
     * output capacity. The live instance still forwards inserted resources to
     * this bus buffer, while the copied instance is an ordinary slot snapshot.
     */
    private final class CombinedOutputHandler extends IItemHandlerImpl
            implements IFluidHandler, IExtendedGasHandler {
        private CombinedOutputHandler(int slotCount) {
            super(slotIndexes(slotCount), slotIndexes(slotCount));
            setStackLimit(Integer.MAX_VALUE, slotIndexes(slotCount));
        }

        private void checkSlot(int slot) {
            if (slot < 0 || slot >= maxDistinctResources) {
                throw new IndexOutOfBoundsException("Universal output slot out of range: " + slot);
            }
        }

        @Override
        public int getSlots() {
            return maxDistinctResources;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            checkSlot(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            checkSlot(slot);
            return offerItem(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            checkSlot(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            checkSlot(slot);
            return Integer.MAX_VALUE;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            checkSlot(slot);
            offerItem(stack, false);
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return fluidOutput.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return fluidOutput.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return fluidOutput.drain(resource, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return fluidOutput.drain(maxDrain, doDrain);
        }

        @Override
        public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
            return gasOutput.receiveGas(side, stack, doTransfer);
        }

        @Override
        public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
            return gasOutput.drawGas(side, amount, doTransfer);
        }

        @Override
        public GasStack drawGas(GasStack request, boolean doTransfer) {
            return gasOutput.drawGas(request, doTransfer);
        }

        @Override
        public boolean canReceiveGas(EnumFacing side, Gas gas) {
            return gasOutput.canReceiveGas(side, gas);
        }

        @Override
        public boolean canDrawGas(EnumFacing side, Gas gas) {
            return gasOutput.canDrawGas(side, gas);
        }

        @Override
        public mekanism.api.gas.GasTankInfo[] getTankInfo() {
            return gasOutput.getTankInfo();
        }
    }

    private final class FluidOutputHandler implements IFluidHandler {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return fluidOutputProperties.clone();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            debug("fluid fill doFill=" + doFill + " stack="
                    + (resource == null || resource.getFluid() == null ? "null"
                    : resource.getFluid().getName() + "x" + resource.amount));
            return offerFluid(resource, !doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }
    }

    private final class GasOutputHandler implements IExtendedGasHandler {
        @Override
        public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
            debug("gas receive doTransfer=" + doTransfer + " stack="
                    + (stack == null || stack.getGas() == null ? "null"
                    : stack.getGas().getName() + "x" + stack.amount));
            return offerGas(stack, !doTransfer);
        }

        @Override
        public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
            return null;
        }

        @Override
        public GasStack drawGas(GasStack request, boolean doTransfer) {
            return null;
        }

        @Override
        public boolean canReceiveGas(EnumFacing side, Gas gas) {
            return gas != null;
        }

        @Override
        public boolean canDrawGas(EnumFacing side, Gas gas) {
            return false;
        }

        @Override
        public GasTankInfo[] getTankInfo() {
            return gasOutputTanks.clone();
        }
    }
}
