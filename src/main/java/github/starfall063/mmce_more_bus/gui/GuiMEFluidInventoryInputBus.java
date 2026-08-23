package github.starfall063.mmce_more_bus.gui;

import appeng.api.storage.data.IAEFluidStack;
import appeng.fluids.client.gui.widgets.GuiFluidSlot;
import appeng.fluids.util.AEFluidStack;
import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.container.ContainerMEFluidInventoryInputBus;
import github.starfall063.mmce_more_bus.module.mmce.me.MEItemInventoryNetwork;
import github.starfall063.mmce_more_bus.tile.MEFluidInventoryInputBus;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public final class GuiMEFluidInventoryInputBus extends GuiMEInventoryInputBusBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/meiteminventoryinputbus.png"
    );
    private static final int SLOT_COLUMNS = 8;
    private static final int FIRST_SLOT_X = 17;
    private static final int FIRST_MARKER_Y = 20;
    private static final int FIRST_PREVIEW_Y = 38;
    private static final int PAIR_GROUP_HEIGHT = 52;
    private static final int INTERNAL_SLOT_COUNT = MEFluidInventoryInputBus.SLOT_COUNT * 2;

    private final MEFluidInventoryInputBus bus;

    public GuiMEFluidInventoryInputBus(InventoryPlayer inventoryPlayer, MEFluidInventoryInputBus bus) {
        super(new ContainerMEFluidInventoryInputBus(inventoryPlayer, bus), bus.getPos(), bus::getMinStackSize, TEXTURE);
        this.bus = bus;
    }

    public static boolean usesSquarePreviewSlots() {
        return true;
    }

    static boolean markerSlotsHideQuantityOverlay() {
        return true;
    }

    private static int slotX(int slot) {
        return FIRST_SLOT_X - 1 + slot % SLOT_COLUMNS * 18;
    }

    private static int markerY(int slot) {
        return FIRST_MARKER_Y - 1 + slot / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    private static int previewY(int slot) {
        return FIRST_PREVIEW_Y - 1 + slot / SLOT_COLUMNS * PAIR_GROUP_HEIGHT;
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    @Override
    public void initGui() {
        super.initGui();
        for (int slot = 0; slot < MEFluidInventoryInputBus.SLOT_COUNT; slot++) {
            getGuiSlots().add(new MarkerFluidSlot(slot, slotX(slot) + 1, markerY(slot) + 1));
        }
    }

    @Override
    public void drawFG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.mmce_more_bus.me_fluid_inventory_input_bus.title"), 8, 8, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 93, 0x404040);
        super.drawFG(guiLeft, guiTop, mouseX, mouseY);
    }

    @Override
    public void drawSlot(Slot slot) {
        if (slot.slotNumber >= INTERNAL_SLOT_COUNT) {
            drawInheritedSlot(slot);
            return;
        }
        if ((slot.slotNumber & 1) == 0) return;

        int markerSlot = slot.slotNumber / 2;
        renderPreviewResource(
                PreviewResource.fluid(bus.getMarker(markerSlot), bus.getVirtualAmount(markerSlot)),
                slot.xPos - 1,
                slot.yPos - 1
        );
    }

    @Override
    public void drawBG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        super.drawBG(guiLeft, guiTop, mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawResourceTooltip(mouseX, mouseY);
        drawSettingsPanel(mouseX, mouseY);
    }

    private void drawResourceTooltip(int mouseX, int mouseY) {
        for (int slot = 0; slot < MEFluidInventoryInputBus.SLOT_COUNT; slot++) {
            int x = guiLeft + slotX(slot);
            int previewY = guiTop + previewY(slot);
            FluidStack marker = bus.getMarker(slot);

            long amount = bus.getVirtualAmount(slot);
            if (marker != null && amount > 0L && isInside(mouseX, mouseY, x, previewY)) {
                drawPreviewTooltip(PreviewResource.fluid(marker, amount), mouseX, mouseY);
                return;
            }
        }
    }

    private final class MarkerFluidSlot extends GuiFluidSlot {
        private final int markerSlot;

        private MarkerFluidSlot(int markerSlot, int x, int y) {
            super(null, markerSlot, markerSlot, x, y);
            this.markerSlot = markerSlot;
        }

        @Override
        public IAEFluidStack getFluidStack() {
            FluidStack marker = bus.getMarker(markerSlot);
            return marker == null ? null : AEFluidStack.fromFluidStack(marker);
        }

        @Override
        public void setFluidStack(IAEFluidStack stack) {
            FluidStack marker = stack == null ? null : stack.getFluidStack();
            if (marker == null) bus.clearMarker(markerSlot);
            else bus.setMarker(markerSlot, marker);
            MEItemInventoryNetwork.CHANNEL.sendToServer(
                    new MEItemInventoryNetwork.SetFluidMarkerMessage(bus.getPos(), markerSlot, marker)
            );
        }
    }
}
