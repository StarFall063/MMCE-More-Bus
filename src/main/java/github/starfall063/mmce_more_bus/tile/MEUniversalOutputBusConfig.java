package github.starfall063.mmce_more_bus.tile;

import com.cleanroommc.configanytime.ConfigAnytime;
import github.starfall063.mmce_more_bus.Tags;
import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, name = "mmce_more_bus/me_universal_output_bus")
public final class MEUniversalOutputBusConfig {
    @Config.Name("MaximumDistinctResources")
    @Config.Comment({
            "Maximum number of different item, fluid, and gas identities buffered by one universal ME output bus.",
            "单个通用 ME 输出总线可缓冲的物品、流体和气体种类上限。"
    })
    @Config.RangeInt(min = 1, max = 8192)
    public static int maximumDistinctResources = MEUniversalOutputBus.DEFAULT_MAX_DISTINCT_RESOURCES;

    private MEUniversalOutputBusConfig() {
    }

    public static void init() {
        ConfigAnytime.register(MEUniversalOutputBusConfig.class);
    }

    static int maxDistinctResources(int value) {
        return Math.max(1, value);
    }
}
