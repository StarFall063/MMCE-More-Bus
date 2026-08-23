package github.starfall063.mmce_more_bus.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.ITickManager;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.me.GridAccessException;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;

/**
 * Shared lifecycle for ME input buses.
 * <p>
 * Resource channels, virtual handlers, snapshots and persistence stay in the
 * concrete bus because their APIs and identities differ between items,
 * fluids, gases and filtered item inputs.
 */
public abstract class AbstractMEInputBus extends MEMachineComponent implements IGridTickable {

    protected static final int DEFAULT_MIN_POLLING_INTERVAL = 10;
    protected static final int DEFAULT_MAX_POLLING_INTERVAL = 120;

    protected abstract boolean hasActiveConfiguration();

    protected abstract void refreshSnapshot();

    protected int getMinimumPollingInterval() {
        return DEFAULT_MIN_POLLING_INTERVAL;
    }

    protected int getMaximumPollingInterval() {
        return DEFAULT_MAX_POLLING_INTERVAL;
    }

    @Override
    public boolean canGroupInput() {
        return true;
    }

    @Override
    public final TickingRequest getTickingRequest(IGridNode node) {
        int minimum = Math.max(1, getMinimumPollingInterval());
        int maximum = Math.max(minimum, getMaximumPollingInterval());
        return new TickingRequest(minimum, maximum, !hasActiveConfiguration(), true);
    }

    @Override
    public final TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        refreshSnapshot();
        return TickRateModulation.SAME;
    }

    protected final void alertTickingDevice() {
        if (!getProxy().isActive()) return;
        try {
            ITickManager tickManager = getProxy().getTick();
            tickManager.alertDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
            // The proxy can disconnect between the active check and this call.
        }
    }
}
