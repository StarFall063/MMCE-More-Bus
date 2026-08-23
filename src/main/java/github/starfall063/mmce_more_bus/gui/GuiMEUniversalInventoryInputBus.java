package github.starfall063.mmce_more_bus.gui;

import appeng.api.storage.data.IAEFluidStack;
import appeng.client.gui.widgets.GuiCustomSlot;
import appeng.fluids.client.gui.widgets.GuiFluidSlot;
import appeng.fluids.util.AEFluidStack;
import com.mekeng.github.client.slots.SlotGas;
import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.container.ContainerMEUniversalInventoryInputBus;
import github.starfall063.mmce_more_bus.module.mmce.me.MEItemInventoryNetwork;
import github.starfall063.mmce_more_bus.tile.MEUniversalInventoryInputBus;
import mekanism.api.gas.GasStack;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GuiMEUniversalInventoryInputBus extends GuiMEInventoryInputBusBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/meiteminventoryinputbus.png"
    );
    private static final int INTERNAL_SLOT_COUNT = MEUniversalInventoryInputBus.SLOT_COUNT * 2;
    private static final int SLOT_COLUMNS = 8;
    private static final int FIRST_SLOT_X = 17;
    private static final int FIRST_MARKER_Y = 20;
    private static final int FIRST_PREVIEW_Y = 38;
    private static final int PAIR_GROUP_HEIGHT = 52;
    private final MEUniversalInventoryInputBus bus;
    private final List<GuiCustomSlot> resourceMarkerSlots = new ArrayList<>();
    private MEUniversalInventoryInputBus.MarkerType[] renderedMarkerTypes;

    public GuiMEUniversalInventoryInputBus(InventoryPlayer inventoryPlayer, MEUniversalInventoryInputBus bus) {
        super(new ContainerMEUniversalInventoryInputBus(inventoryPlayer, bus), bus.getPos(), bus::getMinStackSize, TEXTURE);
        this.bus = bus;
    }

    static boolean markerSlotsHideQuantityOverlay() {
        return true;
    }

    static boolean usesSixteenChannelPairs() {
        return true;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    private static int slotX(int channel) {
        return FIRST_SLOT_X + channel % SLOT_COLUMNS * 18;
    }

    private static int markerY(int channel) {
        return FIRST_MARKER_Y + channel / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    private static int previewY(int channel) {
        return FIRST_PREVIEW_Y + channel / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    static int itemRenderCoordinate(int slotCoordinate) {
        return slotCoordinate;
    }

    static int fluidGasWidgetCoordinate(int slotCoordinate) {
        return slotCoordinate - 1;
    }

    @Override
    public void initGui() {
        super.initGui();
        renderedMarkerTypes = null;
        syncResourceMarkerSlots();
    }

    @Override
    public void drawFG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.mmce_more_bus.me_universal_inventory_input_bus.title"), 8, 8, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 93, 0x404040);
        super.drawFG(guiLeft, guiTop, mouseX, mouseY);
    }

    @Override
    public void drawBG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        super.drawBG(guiLeft, guiTop, mouseX, mouseY);
    }

    @Override
    public void drawSlot(Slot slot) {
        if (slot.slotNumber >= INTERNAL_SLOT_COUNT) {
            drawInheritedSlot(slot);
            return;
        }

        int channel = slot.slotNumber / 2;
        if ((slot.slotNumber & 1) == 0) {
            if (bus.getMarkerType(channel) == MEUniversalInventoryInputBus.MarkerType.ITEM) {
                renderInputItemSlot(slot, 0L);
            }
            return;
        }

        int amount = bus.getPreviewAmount(channel);
        MEUniversalInventoryInputBus.MarkerType type = bus.getMarkerType(channel);
        if (type == MEUniversalInventoryInputBus.MarkerType.ITEM) {
            renderPreviewResource(
                    PreviewResource.item(bus.getItemMarker(channel), amount),
                    slot.xPos,
                    slot.yPos
            );
        } else if (type == MEUniversalInventoryInputBus.MarkerType.FLUID) {
            FluidStack fluid = bus.getFluidMarker(channel).copy();
            fluid.amount = 1;
            renderPreviewResource(
                    PreviewResource.fluid(fluid, amount),
                    fluidGasWidgetCoordinate(slot.xPos),
                    fluidGasWidgetCoordinate(slot.yPos)
            );
        } else if (type == MEUniversalInventoryInputBus.MarkerType.GAS) {
            GasStack gas = bus.getGasMarker(channel).copy();
            gas.amount = 1;
            renderPreviewResource(
                    PreviewResource.gas(gas, amount),
                    fluidGasWidgetCoordinate(slot.xPos),
                    fluidGasWidgetCoordinate(slot.yPos)
            );
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        syncResourceMarkerSlots();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawSettingsPanel(mouseX, mouseY);
        drawResourceTooltip(mouseX, mouseY);
    }

    private void drawResourceTooltip(int mouseX, int mouseY) {
        for (int channel = 0; channel < MEUniversalInventoryInputBus.SLOT_COUNT; channel++) {
            int x = guiLeft + slotX(channel);
            int markerY = guiTop + markerY(channel);
            int previewY = guiTop + previewY(channel);
            if (bus.getMarkerType(channel) == MEUniversalInventoryInputBus.MarkerType.ITEM
                    && inside(mouseX, mouseY, x, markerY)) {
                drawIsolatedHoveringText(Arrays.asList(resourceName(channel)), mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, x, previewY) && bus.getPreviewAmount(channel) > 0) {
                if (bus.getMarkerType(channel) == MEUniversalInventoryInputBus.MarkerType.ITEM) {
                    drawPreviewTooltip(
                            PreviewResource.item(bus.getItemMarker(channel), bus.getPreviewAmount(channel)),
                            mouseX,
                            mouseY
                    );
                    return;
                }
                if (bus.getMarkerType(channel) == MEUniversalInventoryInputBus.MarkerType.FLUID) {
                    drawPreviewTooltip(
                            PreviewResource.fluid(bus.getFluidMarker(channel), bus.getPreviewAmount(channel)),
                            mouseX,
                            mouseY
                    );
                } else if (bus.getMarkerType(channel) == MEUniversalInventoryInputBus.MarkerType.GAS) {
                    drawPreviewTooltip(
                            PreviewResource.gas(bus.getGasMarker(channel), bus.getPreviewAmount(channel)),
                            mouseX,
                            mouseY
                    );
                }
                return;
            }
        }
    }

    private String resourceName(int channel) {
        switch (bus.getMarkerType(channel)) {
            case ITEM:
                return bus.getItemMarker(channel).getDisplayName();
            case FLUID:
                return bus.getFluidMarker(channel).getLocalizedName();
            case GAS:
                return bus.getGasMarker(channel).getGas().getLocalizedName();
            default:
                return "";
        }
    }

    private void syncResourceMarkerSlots() {
        MEUniversalInventoryInputBus.MarkerType[] currentTypes = new MEUniversalInventoryInputBus.MarkerType[
                MEUniversalInventoryInputBus.SLOT_COUNT
                ];
        for (int channel = 0; channel < currentTypes.length; channel++) {
            currentTypes[channel] = bus.getMarkerType(channel);
        }
        if (Arrays.equals(renderedMarkerTypes, currentTypes)) return;

        getGuiSlots().removeAll(resourceMarkerSlots);
        resourceMarkerSlots.clear();
        renderedMarkerTypes = currentTypes;
        for (int channel = 0; channel < currentTypes.length; channel++) {
            MEUniversalInventoryInputBus.MarkerType type = currentTypes[channel];
            if (type == MEUniversalInventoryInputBus.MarkerType.FLUID) {
                addResourceMarkerSlot(new MarkerFluidSlot(channel, fluidGasWidgetCoordinate(slotX(channel)),
                        fluidGasWidgetCoordinate(markerY(channel))));
            } else if (type == MEUniversalInventoryInputBus.MarkerType.GAS) {
                addResourceMarkerSlot(new MarkerGasSlot(channel, fluidGasWidgetCoordinate(slotX(channel)),
                        fluidGasWidgetCoordinate(markerY(channel))));
            }
        }
    }

    private void addResourceMarkerSlot(GuiCustomSlot slot) {
        resourceMarkerSlots.add(slot);
        getGuiSlots().add(slot);
    }

    private final class MarkerFluidSlot extends GuiFluidSlot {
        private final int channel;

        private MarkerFluidSlot(int channel, int x, int y) {
            super(null, channel, channel, x + 1, y + 1);
            this.channel = channel;
        }

        @Override
        public IAEFluidStack getFluidStack() {
            FluidStack marker = bus.getFluidMarker(channel);
            return bus.getMarkerType(channel) == MEUniversalInventoryInputBus.MarkerType.FLUID && marker != null
                    ? AEFluidStack.fromFluidStack(marker)
                    : null;
        }

        @Override
        public void setFluidStack(IAEFluidStack stack) {
            FluidStack marker = stack == null ? null : stack.getFluidStack();
            bus.clearMarker(channel);
            if (marker != null) bus.setFluidMarker(channel, marker);
            MEItemInventoryNetwork.CHANNEL.sendToServer(
                    new MEItemInventoryNetwork.SetUniversalFluidMarkerMessage(bus.getPos(), channel, marker)
            );
        }

        @Override
        public void drawContent(net.minecraft.client.Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
            try {
                super.drawContent(minecraft, mouseX, mouseY, partialTicks);
            } finally {
                restoreNativeResourceRenderState();
            }
        }
    }

    private final class MarkerGasSlot extends SlotGas {
        private final int channel;

        private MarkerGasSlot(int channel, int x, int y) {
            super(null, channel, channel, x + 1, y + 1);
            this.channel = channel;
        }

        @Override
        public GasStack getGasStack() {
            return bus.getMarkerType(channel) == MEUniversalInventoryInputBus.MarkerType.GAS
                    ? bus.getGasMarker(channel)
                    : null;
        }

        @Override
        public void setGasStack(GasStack marker) {
            bus.clearMarker(channel);
            if (marker != null) bus.setGasMarker(channel, marker);
            MEItemInventoryNetwork.CHANNEL.sendToServer(
                    new MEItemInventoryNetwork.SetUniversalGasMarkerMessage(bus.getPos(), channel, marker)
            );
        }

        @Override
        public void drawContent(net.minecraft.client.Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
            try {
                super.drawContent(minecraft, mouseX, mouseY, partialTicks);
            } finally {
                restoreNativeResourceRenderState();
            }
        }
    }
}
