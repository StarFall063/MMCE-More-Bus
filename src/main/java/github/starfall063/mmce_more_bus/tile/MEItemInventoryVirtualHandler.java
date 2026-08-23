package github.starfall063.mmce_more_bus.tile;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Read-only item handler exposing the latest worker-safe ME availability snapshot.
 */
public final class MEItemInventoryVirtualHandler implements IItemHandlerModifiable {
    private final AtomicReference<MEItemInventorySnapshot> snapshot;
    private final NetworkExtractor networkExtractor;

    public MEItemInventoryVirtualHandler(MEItemInventorySnapshot initialSnapshot) {
        this(initialSnapshot, (slot, amount) -> ItemStack.EMPTY);
    }

    public MEItemInventoryVirtualHandler(MEItemInventorySnapshot initialSnapshot, NetworkExtractor networkExtractor) {
        this.snapshot = new AtomicReference<>(initialSnapshot);
        this.networkExtractor = networkExtractor;
    }

    public void setSnapshot(MEItemInventorySnapshot replacement) {
        snapshot.set(replacement);
    }

    @Override
    public int getSlots() {
        return snapshot.get().size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return snapshot.get().getVirtualStack(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        ItemStack available = getStackInSlot(slot);
        if (available.isEmpty()) return ItemStack.EMPTY;

        int extractedAmount = Math.min(amount, available.getCount());
        if (!simulate) return networkExtractor.extract(slot, extractedAmount);

        ItemStack extracted = available.copy();
        extracted.setCount(extractedAmount);
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        ItemStack available = getStackInSlot(slot);
        if (available.isEmpty()) return;
        if (!stack.isEmpty() && (!ItemStack.areItemsEqual(available, stack)
                || !ItemStack.areItemStackTagsEqual(available, stack))) {
            return;
        }

        int remaining = stack.isEmpty() ? 0 : Math.max(0, stack.getCount());
        int consumed = available.getCount() - remaining;
        if (consumed > 0) networkExtractor.extract(slot, consumed);
    }

    @FunctionalInterface
    public interface NetworkExtractor {
        ItemStack extract(int slot, int amount);
    }
}
