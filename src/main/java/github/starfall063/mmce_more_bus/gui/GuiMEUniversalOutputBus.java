package github.starfall063.mmce_more_bus.gui;

import appeng.client.gui.widgets.GuiScrollbar;
import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.container.ContainerMEUniversalOutputBus;
import github.starfall063.mmce_more_bus.module.mmce.me.MEItemInventoryNetwork;
import github.starfall063.mmce_more_bus.tile.MEUniversalOutputBus;
import mekanism.api.gas.GasStack;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class GuiMEUniversalOutputBus extends GuiMEInventoryInputBusBase {
    private static final int GUI_WIDTH = 195;
    private static final int GUI_HEIGHT = 195;
    private static final int RESOURCE_COLUMNS = 9;
    private static final int RESOURCE_COUNT = 36;
    private static final int RESOURCE_ROWS = (RESOURCE_COUNT + RESOURCE_COLUMNS - 1) / RESOURCE_COLUMNS;
    private static final int RESOURCE_SLOT_X = 8;
    private static final int RESOURCE_SLOT_Y = 24;
    private static final int RESOURCE_SLOT_STEP = 18;
    private static final int SCROLLBAR_HEIGHT = RESOURCE_ROWS * RESOURCE_SLOT_STEP - 2;
    private static final int SCROLLBAR_X = RESOURCE_SLOT_X + RESOURCE_COLUMNS * RESOURCE_SLOT_STEP + 5;
    private static final int SCROLLBAR_Y = 24;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int PLAYER_LABEL_Y = 101;
    private static final int VIEWPORT_REFRESH_INTERVAL_TICKS = 10;
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/meuniversaloutputbus.png"
    );
    private final MEUniversalOutputBus bus;
    private final GuiScrollbar outputScrollbar;
    private int viewportRefreshTicks;
    private int lastInteractionRowOffset = -1;

    public GuiMEUniversalOutputBus(InventoryPlayer inventoryPlayer, MEUniversalOutputBus bus) {
        super(new ContainerMEUniversalOutputBus(inventoryPlayer, bus), bus.getPos(), () -> 0, TEXTURE);
        this.bus = bus;
        xSize = GUI_WIDTH;
        ySize = GUI_HEIGHT;
        outputScrollbar = new GuiScrollbar();
        setScrollBar(outputScrollbar);
        widgetController = createWidgetController(WidgetGui.of(this, xSize, ySize, guiLeft, guiTop));
    }

    static boolean shouldRefreshViewport(int elapsedTicks) {
        return elapsedTicks >= VIEWPORT_REFRESH_INTERVAL_TICKS;
    }

    static WidgetController createWidgetController(WidgetGui gui) {
        return new WidgetController(gui);
    }

    private static int resourceSlotX(int slot) {
        return RESOURCE_SLOT_X + slot % RESOURCE_COLUMNS * RESOURCE_SLOT_STEP;
    }

    private static int resourceSlotY(int slot) {
        return RESOURCE_SLOT_Y + slot / RESOURCE_COLUMNS * RESOURCE_SLOT_STEP;
    }

    static boolean isBaseHoverableSlot(int slotNumber) {
        return slotNumber < 0 || slotNumber >= RESOURCE_COUNT;
    }

    static List<String> itemTooltipWithExactAmount(List<String> tooltip, long amount) {
        return GuiMEInventoryInputBusBase.appendStoredAmount(tooltip, amount);
    }

    private static String resourceName(MEUniversalOutputBus.DisplayResource resource) {
        ItemStack item = resource.getItem();
        if (!item.isEmpty()) return item.getDisplayName();
        FluidStack fluid = resource.getFluid();
        if (fluid != null) return fluid.getLocalizedName();
        GasStack gas = resource.getGas();
        return gas == null ? "" : gas.getGas().getLocalizedName();
    }

    private static String exactAmount(MEUniversalOutputBus.DisplayResource resource) {
        if (!resource.getItem().isEmpty()) {
            return GuiMEInventoryInputBusBase.storedAmountText(resource.getAmount());
        }
        return GuiMEInventoryInputBusBase.storedFluidAmountText(resource.getAmount());
    }

    @Override
    public void initGui() {
        super.initGui();
        outputScrollbar.setLeft(SCROLLBAR_X);
        outputScrollbar.setTop(SCROLLBAR_Y);
        outputScrollbar.setWidth(SCROLLBAR_WIDTH);
        outputScrollbar.setHeight(SCROLLBAR_HEIGHT);
        syncScrollbarRange();
        requestViewport(0);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        requestViewportAfterScrollbarInput();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        viewportRefreshTicks++;
        syncScrollbarRange();
        if (!shouldRefreshViewport(viewportRefreshTicks)) return;
        viewportRefreshTicks = 0;
        requestViewport(outputScrollbar.getCurrentScroll());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        requestViewportAfterScrollbarInput();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        requestViewportAfterScrollbarInput();
    }

    @Override
    public void drawFG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.mmce_more_bus.me_universal_output_bus.title"), 8, 8, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, PLAYER_LABEL_Y, 0x404040);
    }

    @Override
    public void drawBG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawResourceTooltip(mouseX, mouseY);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.slotNumber < RESOURCE_COUNT && !hoveredSlot.getStack().isEmpty()) {
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    public Slot getSlotAtPosition(int mouseX, int mouseY) {
        Slot slot = super.getSlotAtPosition(mouseX, mouseY);
        return slot != null && isBaseHoverableSlot(slot.slotNumber) ? slot : null;
    }

    @Override
    public boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        return slot != null
                && isBaseHoverableSlot(slot.slotNumber)
                && super.isMouseOverSlot(slot, mouseX, mouseY);
    }

    @Override
    public void drawSlot(Slot slot) {
        if (slot.slotNumber >= RESOURCE_COUNT) {
            drawInheritedSlot(slot);
            return;
        }

        List<MEUniversalOutputBus.DisplayResource> viewport = bus.getClientViewport();
        if (slot.slotNumber >= viewport.size()) return;
        renderResource(viewport.get(slot.slotNumber), slot.xPos, slot.yPos);
    }

    private void syncScrollbarRange() {
        outputScrollbar.setRange(0, bus.getClientViewportMaxRowOffset(), 1);
    }

    private void requestViewportAfterScrollbarInput() {
        int rowOffset = outputScrollbar.getCurrentScroll();
        if (rowOffset == lastInteractionRowOffset) return;
        lastInteractionRowOffset = rowOffset;
        requestViewport(rowOffset);
    }

    private void renderResource(MEUniversalOutputBus.DisplayResource resource, int x, int y) {
        ItemStack item = resource.getItem();
        if (!item.isEmpty()) {
            renderPreviewResource(PreviewResource.item(item, resource.getAmount()), x, y);
            return;
        }

        FluidStack fluid = resource.getFluid();
        if (fluid != null) {
            FluidStack displayed = fluid.copy();
            displayed.amount = 1;
            renderPreviewResource(PreviewResource.fluid(displayed, resource.getAmount()), x - 1, y - 1);
            return;
        }

        GasStack gas = resource.getGas();
        if (gas != null) {
            GasStack displayed = gas.copy();
            displayed.amount = 1;
            renderPreviewResource(PreviewResource.gas(displayed, resource.getAmount()), x - 1, y - 1);
        }
    }

    private void drawResourceTooltip(int mouseX, int mouseY) {
        List<MEUniversalOutputBus.DisplayResource> resources = bus.getClientViewport();
        for (int index = 0; index < Math.min(RESOURCE_COUNT, resources.size()); index++) {
            int x = guiLeft + resourceSlotX(index);
            int y = guiTop + resourceSlotY(index);
            if (mouseX < x || mouseX >= x + 18 || mouseY < y || mouseY >= y + 18) continue;

            MEUniversalOutputBus.DisplayResource resource = resources.get(index);
            ItemStack item = resource.getItem();
            if (!item.isEmpty()) {
                drawIsolatedHoveringText(itemTooltipWithExactAmount(getItemToolTip(item), resource.getAmount()), mouseX, mouseY);
                return;
            }
            drawIsolatedHoveringText(Arrays.asList(resourceName(resource), exactAmount(resource)), mouseX, mouseY);
            return;
        }
    }

    private void requestViewport(int requestedRowOffset) {
        requestedRowOffset = Math.max(0, Math.min(requestedRowOffset, bus.getClientViewportMaxRowOffset()));
        MEItemInventoryNetwork.CHANNEL.sendToServer(
                new MEItemInventoryNetwork.RequestOutputViewportMessage(
                        bus.getPos(),
                        requestedRowOffset,
                        bus.getClientViewportRevision(),
                        bus.getClientViewportRowOffset()
                )
        );
    }
}
