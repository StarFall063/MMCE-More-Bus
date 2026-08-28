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
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;

/**
 * Item-only ME input bus that expands one OreDictionary name into up to sixteen
 * live AE item inputs.
 */
public final class MEOreDictionaryInputBus extends AbstractMarkerMEInputBus<ItemStack>
        implements MachineComponentDropState {
    public static final int SLOT_COUNT = AbstractMarkerMEInputBus.SLOT_COUNT;
    public static final int PULL_BY_AMOUNT = 0;
    public static final int PULL_BY_NAME = 1;
    public static final int PULL_BY_MOD = 2;
    public static final int MATCH_EXACT = 0;
    public static final int MATCH_PREFIX = 1;
    public static final int DEFAULT_MATCHING_MODE = MATCH_EXACT;
    public static final int MAX_ORE_EXPRESSION_LENGTH = 128;

    private static final String KEY_STATE = "sfc_me_oredict_input_bus_state";
    private static final String KEY_ORE_NAME = "sfc_ore_dictionary_name";
    private static final String KEY_PULL_MODE = "sfc_pull_mode";
    private static final String KEY_MATCHING_MODE = "sfc_matching_mode";
    private MEItemInventorySnapshot snapshot = MEItemInventorySnapshot.empty();
    private String oreDictionaryName = "";    private final MEItemInventoryVirtualHandler virtualHandler = new MEItemInventoryVirtualHandler(
            MEItemInventorySnapshot.empty(),
            this::extractFromNetwork
    );
    private int pullMode = PULL_BY_AMOUNT;
    private int matchingMode = DEFAULT_MATCHING_MODE;
    private String compiledExpression;
    private int compiledMatchingMode = -1;
    private Predicate<String> compiledOreNameMatcher;

    static String normalizeOreDictionaryName(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        return normalized.length() <= MAX_ORE_EXPRESSION_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ORE_EXPRESSION_LENGTH);
    }

    static int normalizePullMode(int value) {
        return value >= PULL_BY_AMOUNT && value <= PULL_BY_MOD ? value : PULL_BY_AMOUNT;
    }

    static int normalizeMatchingMode(int value) {
        return value >= MATCH_EXACT && value <= MATCH_PREFIX ? value : DEFAULT_MATCHING_MODE;
    }

    static int normalizeMinimumStock(int value) {
        return Math.max(1, value);
    }

    static int candidateLimit(int candidateCount) {
        return Math.min(SLOT_COUNT, Math.max(0, candidateCount));
    }

    static boolean matchesOreDictionary(ItemStack stack, String oreName) {
        return matchesOreDictionary(stack, oreName, MATCH_EXACT);
    }

    static boolean matchesOreDictionary(ItemStack stack, String expression, int mode) {
        return matchesOreDictionary(stack, compileOreNameMatcher(expression, mode));
    }

    static boolean matchesOreExpression(String candidate, String expression, int mode) {
        Predicate<String> matcher = compileOreNameMatcher(expression, mode);
        return candidate != null && matcher != null && matcher.test(candidate);
    }

    private static boolean matchesOreDictionary(ItemStack stack, Predicate<String> oreNameMatcher) {
        if (stack == null || stack.isEmpty() || oreNameMatcher == null) return false;
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            if (oreNameMatcher.test(OreDictionary.getOreName(oreId))) return true;
        }
        return false;
    }

    static boolean matchesOreName(String candidate, String query, int mode) {
        if (candidate == null || query == null || query.isEmpty()) return false;
        if (containsGlob(query)) return matchesGlob(candidate, query);
        return normalizeMatchingMode(mode) == MATCH_PREFIX
                ? candidate.startsWith(query)
                : candidate.equals(query);
    }

    private static Predicate<String> compileOreNameMatcher(String expression, int mode) {
        if (expression == null || expression.trim().isEmpty()) return null;
        String normalized = expression.trim();
        int[] cursor = {0};
        try {
            Predicate<String> matcher = parseOrExpression(normalized, cursor, normalizeMatchingMode(mode));
            skipWhitespace(normalized, cursor);
            return cursor[0] == normalized.length() ? matcher : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Predicate<String> parseOrExpression(String expression, int[] cursor, int mode) {
        Predicate<String> result = parseAndExpression(expression, cursor, mode);
        while (consume(expression, cursor, '|')) {
            Predicate<String> left = result;
            Predicate<String> right = parseAndExpression(expression, cursor, mode);
            result = candidate -> left.test(candidate) || right.test(candidate);
        }
        return result;
    }

    private static Predicate<String> parseAndExpression(String expression, int[] cursor, int mode) {
        Predicate<String> result = parseUnaryExpression(expression, cursor, mode);
        while (consume(expression, cursor, '&')) {
            Predicate<String> left = result;
            Predicate<String> right = parseUnaryExpression(expression, cursor, mode);
            result = candidate -> left.test(candidate) && right.test(candidate);
        }
        return result;
    }

    private static Predicate<String> parseUnaryExpression(String expression, int[] cursor, int mode) {
        if (consume(expression, cursor, '!')) {
            Predicate<String> operand = parseUnaryExpression(expression, cursor, mode);
            return candidate -> !operand.test(candidate);
        }
        if (consume(expression, cursor, '(')) {
            Predicate<String> grouped = parseOrExpression(expression, cursor, mode);
            if (!consume(expression, cursor, ')')) throw new IllegalArgumentException("Missing closing parenthesis");
            return grouped;
        }
        return parseAtom(expression, cursor, mode);
    }

    private static Predicate<String> parseAtom(String expression, int[] cursor, int mode) {
        skipWhitespace(expression, cursor);
        int start = cursor[0];
        while (cursor[0] < expression.length() && !isOperator(expression.charAt(cursor[0]))) {
            cursor[0]++;
        }
        String atom = expression.substring(start, cursor[0]).trim();
        if (atom.isEmpty()) throw new IllegalArgumentException("Missing expression atom");
        return candidate -> matchesOreName(candidate, atom, mode);
    }

    private static boolean consume(String expression, int[] cursor, char expected) {
        skipWhitespace(expression, cursor);
        if (cursor[0] >= expression.length() || expression.charAt(cursor[0]) != expected) return false;
        cursor[0]++;
        return true;
    }

    private static void skipWhitespace(String expression, int[] cursor) {
        while (cursor[0] < expression.length() && Character.isWhitespace(expression.charAt(cursor[0]))) {
            cursor[0]++;
        }
    }

    private static boolean isOperator(char value) {
        return value == '|' || value == '&' || value == '!' || value == '(' || value == ')';
    }

    private static boolean containsGlob(String value) {
        return value.indexOf('*') >= 0 || value.indexOf('?') >= 0;
    }

    private static boolean matchesGlob(String candidate, String pattern) {
        int candidateIndex = 0;
        int patternIndex = 0;
        int wildcardIndex = -1;
        int wildcardCandidateIndex = -1;

        while (candidateIndex < candidate.length()) {
            if (patternIndex < pattern.length()
                    && (pattern.charAt(patternIndex) == '?' || pattern.charAt(patternIndex) == candidate.charAt(candidateIndex))) {
                candidateIndex++;
                patternIndex++;
            } else if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                wildcardIndex = patternIndex++;
                wildcardCandidateIndex = candidateIndex;
            } else if (wildcardIndex >= 0) {
                patternIndex = wildcardIndex + 1;
                candidateIndex = ++wildcardCandidateIndex;
            } else {
                return false;
            }
        }

        while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
            patternIndex++;
        }
        return patternIndex == pattern.length();
    }

    private static String modSortKey(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return "";
        return stack.getItem().getRegistryName().getNamespace();
    }

    private static String identitySortKey(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return "";
        return stack.getItem().getRegistryName()
                + "#" + stack.getMetadata()
                + "#" + String.valueOf(stack.getTagCompound());
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.meItemInputBus);
    }

    @Override
    public MachineComponent.ItemBus provideComponent() {
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public long getGroupID() {
                return MEOreDictionaryInputBus.this.getGroupId();
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public net.minecraftforge.items.IItemHandlerModifiable getContainerProvider() {
                return virtualHandler;
            }
        };
    }

    @Override
    protected boolean hasActiveConfiguration() {
        return !oreDictionaryName.isEmpty();
    }

    @Override
    public boolean hasDropConfiguration() {
        return super.hasDropConfiguration()
                || !oreDictionaryName.isEmpty()
                || pullMode != PULL_BY_AMOUNT
                || matchingMode != DEFAULT_MATCHING_MODE;
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
    protected void refreshSnapshot() {
        long[] amounts = new long[SLOT_COUNT];
        if (!hasActiveConfiguration()) {
            clearGeneratedMarkers();
            publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
            return;
        }

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

        IItemList<IAEItemStack> available = monitor.getAvailableItems(channel.createList());
        Predicate<String> oreNameMatcher = getCompiledOreNameMatcher();
        if (oreNameMatcher == null) {
            clearGeneratedMarkers();
            publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
            return;
        }
        PriorityQueue<IAEItemStack> candidates = new PriorityQueue<>(
                SLOT_COUNT,
                (first, second) -> compareCandidates(second, first)
        );
        for (IAEItemStack entry : available) {
            if (entry.getStackSize() < getMinStackSize()) continue;

            ItemStack stack = entry.createItemStack();
            if (matchesOreDictionary(stack, oreNameMatcher)) {
                if (candidates.size() < SLOT_COUNT) {
                    candidates.offer(entry);
                } else if (compareCandidates(entry, candidates.peek()) < 0) {
                    candidates.poll();
                    candidates.offer(entry);
                }
            }
        }
        List<IAEItemStack> selectedCandidates = new ArrayList<>(candidates);
        selectedCandidates.sort(this::compareCandidates);

        clearGeneratedMarkers();
        int slot = 0;
        int candidateLimit = candidateLimit(selectedCandidates.size());
        for (IAEItemStack candidate : selectedCandidates) {
            if (slot >= candidateLimit) break;

            ItemStack marker = candidate.createItemStack();
            marker.setCount(1);
            if (!setMarkerSilently(slot, marker)) continue;

            amounts[slot] = Math.max(0L, candidate.getStackSize());
            slot++;
        }
        publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), amounts));
    }

    private int compareCandidates(IAEItemStack first, IAEItemStack second) {
        ItemStack firstStack = first.createItemStack();
        ItemStack secondStack = second.createItemStack();
        int result;

        switch (pullMode) {
            case PULL_BY_NAME:
                result = identitySortKey(firstStack).compareTo(identitySortKey(secondStack));
                break;
            case PULL_BY_MOD:
                result = modSortKey(firstStack).compareTo(modSortKey(secondStack));
                if (result == 0) {
                    result = identitySortKey(firstStack).compareTo(identitySortKey(secondStack));
                }
                break;
            case PULL_BY_AMOUNT:
            default:
                result = Long.compare(second.getStackSize(), first.getStackSize());
                break;
        }

        if (result != 0) return result;
        return identitySortKey(firstStack).compareTo(identitySortKey(secondStack));
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

    private void clearGeneratedMarkers() {
        clearAllMarkersSilently();
    }

    private void invalidateConfiguration() {
        invalidateResourceSnapshot();
        markDirty();
        alertTickingDevice();
        if (getWorld() != null) markForUpdateSync();
    }

    public String getOreDictionaryName() {
        return oreDictionaryName;
    }

    public void setOreDictionaryName(String value) {
        String normalized = normalizeOreDictionaryName(value);
        if (oreDictionaryName.equals(normalized)) return;
        oreDictionaryName = normalized;
        invalidateConfiguration();
    }

    public int getPullMode() {
        return pullMode;
    }

    public void setPullMode(int value) {
        int normalized = normalizePullMode(value);
        if (pullMode == normalized) return;
        pullMode = normalized;
        invalidateConfiguration();
    }

    public int getMatchingMode() {
        return matchingMode;
    }

    public void setMatchingMode(int value) {
        int normalized = normalizeMatchingMode(value);
        if (matchingMode == normalized) return;
        matchingMode = normalized;
        invalidateConfiguration();
    }

    public ItemStack getVirtualStack(int slot) {
        return virtualHandler.getStackInSlot(slot);
    }

    public long getVirtualAmount(int slot) {
        return snapshot.getAmount(slot);
    }

    public net.minecraftforge.items.IItemHandlerModifiable getVirtualHandler() {
        return virtualHandler;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readDropState(compound);
    }

    @Override
    public void readDropState(NBTTagCompound compound) {
        if (compound.hasKey(KEY_STATE, 10)) {
            NBTTagCompound stateTag = compound.getCompoundTag(KEY_STATE);
            readMarkerState(stateTag);
            oreDictionaryName = normalizeOreDictionaryName(stateTag.getString(KEY_ORE_NAME));
            pullMode = normalizePullMode(stateTag.getInteger(KEY_PULL_MODE));
            matchingMode = normalizeMatchingMode(stateTag.getInteger(KEY_MATCHING_MODE));
        }
        clearCompiledOreNameMatcher();
        snapshot = MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), new long[SLOT_COUNT]);
        virtualHandler.setSnapshot(snapshot);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        writeDropState(compound);
    }

    @Override
    public void writeDropState(NBTTagCompound compound) {
        NBTTagCompound stateTag = new NBTTagCompound();
        writeMarkerState(stateTag);
        stateTag.setString(KEY_ORE_NAME, oreDictionaryName);
        stateTag.setInteger(KEY_PULL_MODE, pullMode);
        stateTag.setInteger(KEY_MATCHING_MODE, matchingMode);
        compound.setTag(KEY_STATE, stateTag);
    }

    @Override
    public void readNetNBT(NBTTagCompound compound) {
        super.readNetNBT(compound);
        if (compound.hasKey(KEY_STATE, 10)) {
            NBTTagCompound stateTag = compound.getCompoundTag(KEY_STATE);
            readMarkerState(stateTag);
            oreDictionaryName = normalizeOreDictionaryName(stateTag.getString(KEY_ORE_NAME));
            pullMode = normalizePullMode(stateTag.getInteger(KEY_PULL_MODE));
            matchingMode = normalizeMatchingMode(stateTag.getInteger(KEY_MATCHING_MODE));
        }
        clearCompiledOreNameMatcher();
        if (compound.hasKey("sfc_ore_dictionary_input_bus_snapshot", 10)) {
            snapshot = MEItemInventorySnapshot.readNBT(
                    compound.getCompoundTag("sfc_ore_dictionary_input_bus_snapshot")
            );
            Object[] markers = new Object[SLOT_COUNT];
            for (int slot = 0; slot < SLOT_COUNT; slot++) markers[slot] = snapshot.getMarker(slot);
            replaceMarkerState(markers, snapshot.getMinStackSize());
            virtualHandler.setSnapshot(snapshot);
        }
    }

    @Override
    public void writeNetNBT(NBTTagCompound compound) {
        super.writeNetNBT(compound);
        NBTTagCompound stateTag = new NBTTagCompound();
        writeMarkerState(stateTag);
        stateTag.setString(KEY_ORE_NAME, oreDictionaryName);
        stateTag.setInteger(KEY_PULL_MODE, pullMode);
        stateTag.setInteger(KEY_MATCHING_MODE, matchingMode);
        compound.setTag(KEY_STATE, stateTag);
        NBTTagCompound snapshotTag = new NBTTagCompound();
        snapshot.writeNBT(snapshotTag);
        compound.setTag("sfc_ore_dictionary_input_bus_snapshot", snapshotTag);
    }

    @Override
    protected void invalidateResourceSnapshot() {
        clearCompiledOreNameMatcher();
        clearGeneratedMarkers();
        publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), new long[SLOT_COUNT]));
    }

    @Override
    protected void markerStateLoaded() {
        clearCompiledOreNameMatcher();
        publishSnapshot(MEItemInventorySnapshot.from(currentMarkers(), getMinStackSize(), new long[SLOT_COUNT]));
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

    private Predicate<String> getCompiledOreNameMatcher() {
        if (oreDictionaryName.equals(compiledExpression) && matchingMode == compiledMatchingMode) {
            return compiledOreNameMatcher;
        }
        compiledExpression = oreDictionaryName;
        compiledMatchingMode = matchingMode;
        compiledOreNameMatcher = compileOreNameMatcher(oreDictionaryName, matchingMode);
        return compiledOreNameMatcher;
    }

    private void clearCompiledOreNameMatcher() {
        compiledExpression = null;
        compiledMatchingMode = -1;
        compiledOreNameMatcher = null;
    }




}
