package github.starfall063.mmce_more_bus.registry;

import github.starfall063.mmce_more_bus.Tags;
import hellfirepvp.modularmachinery.common.item.ItemDynamicColor;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RegistryItems {
    private static final List<ItemDynamicColor> DYNAMIC_COLOR_ITEMS = new ArrayList<>();

    private RegistryItems() {
    }

    static void trackDynamicColorItem(Item item) {
        if (item instanceof ItemDynamicColor) {
            DYNAMIC_COLOR_ITEMS.add((ItemDynamicColor) item);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        for (ItemDynamicColor dynamicColorItem : DYNAMIC_COLOR_ITEMS) {
            event.getItemColors().registerItemColorHandler(
                    dynamicColorItem::getColorFromItemstack,
                    (Item) dynamicColorItem
            );
        }
    }
}
