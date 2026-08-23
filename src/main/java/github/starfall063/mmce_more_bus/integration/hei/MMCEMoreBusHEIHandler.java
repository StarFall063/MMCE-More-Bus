package github.starfall063.mmce_more_bus.integration.hei;

import github.starfall063.mmce_more_bus.gui.HeiExtraAreaProvider;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.awt.Rectangle;
import java.util.List;

final class MMCEMoreBusHEIHandler<T extends GuiContainer & HeiExtraAreaProvider>
        implements IAdvancedGuiHandler<T> {
    private final Class<T> guiClass;

    MMCEMoreBusHEIHandler(Class<T> guiClass) {
        this.guiClass = guiClass;
    }

    @Override
    public Class<T> getGuiContainerClass() {
        return guiClass;
    }

    @Override
    public List<Rectangle> getGuiExtraAreas(T gui) {
        return gui.getHeiExtraAreas();
    }
}
