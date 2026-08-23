package github.starfall063.mmce_more_bus.gui;

import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.container.ContainerMEItemInventoryInputBus;
import github.starfall063.mmce_more_bus.tile.MEItemInventoryInputBus;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Item-specific rendering over the shared ME input-bus configuration panel.
 */
public final class GuiMEItemInventoryInputBus extends GuiMEInventoryInputBusBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/meiteminventoryinputbus.png"
    );
    private static final int INTERNAL_SLOT_COUNT = MEItemInventoryInputBus.SLOT_COUNT * 2;

    private final MEItemInventoryInputBus bus;

    public GuiMEItemInventoryInputBus(InventoryPlayer inventoryPlayer, MEItemInventoryInputBus bus) {
        super(new ContainerMEItemInventoryInputBus(inventoryPlayer, bus), bus.getPos(), bus::getMinStackSize, TEXTURE);
        this.bus = bus;
    }

    static boolean markerSlotsHideQuantityOverlay() {
        return true;
    }

    @Override
    public void drawFG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.mmce_more_bus.me_item_inventory_input_bus.title"), 8, 8, 0x404040);
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

        renderInputItemSlot(
                slot,
                (slot.slotNumber & 1) == 0 ? 0L : bus.getVirtualAmount(slot.slotNumber / 2)
        );
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawVirtualTooltip(mouseX, mouseY);
        drawSettingsPanel(mouseX, mouseY);
    }

    private void drawVirtualTooltip(int mouseX, int mouseY) {
        Slot slot = findVirtualSlotAt(mouseX, mouseY);
        if (slot == null || slot.getStack().isEmpty()) return;

        List<String> tooltip = new ArrayList<>(getItemToolTip(slot.getStack()));
        drawIsolatedHoveringText(appendStoredAmount(tooltip, bus.getVirtualAmount(slot.slotNumber / 2)), mouseX, mouseY);
    }

    private Slot findVirtualSlotAt(int mouseX, int mouseY) {
        for (Slot slot : inventorySlots.inventorySlots) {
            if (slot.slotNumber >= INTERNAL_SLOT_COUNT || (slot.slotNumber & 1) == 0) continue;
            if (isPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY)) return slot;
        }
        return null;
    }
}
