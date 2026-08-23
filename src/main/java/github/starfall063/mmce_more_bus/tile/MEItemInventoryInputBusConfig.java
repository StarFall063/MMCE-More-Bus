package github.starfall063.mmce_more_bus.tile;

import com.cleanroommc.configanytime.ConfigAnytime;
import github.starfall063.mmce_more_bus.Tags;
import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, name = "mmce_more_bus_config/me_item_inventory_input_bus")
public final class MEItemInventoryInputBusConfig {
    @Config.Name("Polling")
    public static final Polling POLLING = new Polling();

    private MEItemInventoryInputBusConfig() {
    }

    public static void init() {
        ConfigAnytime.register(MEItemInventoryInputBusConfig.class);
    }

    static int minPollingInterval(int minimum, int maximum) {
        return Math.max(1, minimum);
    }

    static int maxPollingInterval(int minimum, int maximum) {
        return Math.max(minPollingInterval(minimum, maximum), Math.max(1, maximum));
    }

    public static final class Polling {
        @Config.Name("MinimumPollingInterval")
        @Config.Comment({
                "Minimum AE network scan interval in ticks.",
                "AE网络扫描的最短间隔，单位为tick。"
        })
        @Config.RangeInt(min = 1, max = 1_200)
        public int minimumPollingInterval = 10;

        @Config.Name("MaximumPollingInterval")
        @Config.Comment({
                "Maximum AE network scan interval in ticks.",
                "AE网络扫描的最长间隔，单位为tick。"
        })
        @Config.RangeInt(min = 1, max = 1_200)
        public int maximumPollingInterval = 120;
    }
}
