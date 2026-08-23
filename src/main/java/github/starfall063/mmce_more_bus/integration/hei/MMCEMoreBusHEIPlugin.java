package github.starfall063.mmce_more_bus.integration.hei;

import github.starfall063.mmce_more_bus.gui.GuiMEFluidInventoryInputBus;
import github.starfall063.mmce_more_bus.gui.GuiMEGasInventoryInputBus;
import github.starfall063.mmce_more_bus.gui.GuiMEItemInventoryInputBus;
import github.starfall063.mmce_more_bus.gui.GuiMEOreDictionaryInputBus;
import github.starfall063.mmce_more_bus.gui.GuiMEUniversalInventoryInputBus;
import github.starfall063.mmce_more_bus.gui.GuiMEUniversalOutputBus;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IAdvancedGuiHandler;

@JEIPlugin
public final class MMCEMoreBusHEIPlugin implements IModPlugin {
    static IAdvancedGuiHandler<GuiMEItemInventoryInputBus> inventoryBusGuiHandler() {
        return new MMCEMoreBusHEIHandler<>(GuiMEItemInventoryInputBus.class);
    }

    static IAdvancedGuiHandler<GuiMEFluidInventoryInputBus> fluidInventoryBusGuiHandler() {
        return new MMCEMoreBusHEIHandler<>(GuiMEFluidInventoryInputBus.class);
    }

    static IAdvancedGuiHandler<GuiMEGasInventoryInputBus> gasInventoryBusGuiHandler() {
        return new MMCEMoreBusHEIHandler<>(GuiMEGasInventoryInputBus.class);
    }

    static IAdvancedGuiHandler<GuiMEOreDictionaryInputBus> oreDictionaryInputBusGuiHandler() {
        return new MMCEMoreBusHEIHandler<>(GuiMEOreDictionaryInputBus.class);
    }

    static IAdvancedGuiHandler<GuiMEUniversalInventoryInputBus> universalInventoryBusGuiHandler() {
        return new MMCEMoreBusHEIHandler<>(GuiMEUniversalInventoryInputBus.class);
    }

    static IAdvancedGuiHandler<GuiMEUniversalOutputBus> universalOutputBusGuiHandler() {
        return new MMCEMoreBusHEIHandler<>(GuiMEUniversalOutputBus.class);
    }

    @Override
    public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(
                inventoryBusGuiHandler(),
                fluidInventoryBusGuiHandler(),
                gasInventoryBusGuiHandler(),
                oreDictionaryInputBusGuiHandler(),
                universalInventoryBusGuiHandler(),
                universalOutputBusGuiHandler()
        );
    }
}
