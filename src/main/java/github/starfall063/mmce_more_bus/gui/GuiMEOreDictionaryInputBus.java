package github.starfall063.mmce_more_bus.gui;

import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.container.ContainerMEOreDictionaryInputBus;
import github.starfall063.mmce_more_bus.module.mmce.me.MEItemInventoryNetwork;
import github.starfall063.mmce_more_bus.tile.MEOreDictionaryInputBus;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GuiMEOreDictionaryInputBus extends GuiMEInventoryInputBusBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/meiteminventoryinputbus.png"
    );
    private static final ResourceLocation SETTINGS_TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/movable_configuration.png"
    );
    private static final ResourceLocation BUTTON_STATE_TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/button_state.png"
    );
    private static final int INTERNAL_SLOT_COUNT = MEOreDictionaryInputBus.SLOT_COUNT * 2;
    private static final int SETTINGS_TEXTURE_WIDTH = 127;
    private static final int SETTINGS_TEXTURE_HEIGHT = 54;
    private static final int CONFIGURATION_TOP_BORDER_HEIGHT = 5;
    private static final int CONFIGURATION_MIDDLE_TEXTURE_Y = CONFIGURATION_TOP_BORDER_HEIGHT;
    private static final int CONFIGURATION_MIDDLE_TEXTURE_HEIGHT = SETTINGS_TEXTURE_HEIGHT - 10;
    private static final int CONFIGURATION_BOTTOM_TEXTURE_Y = SETTINGS_TEXTURE_HEIGHT - 5;
    private static final int CONFIGURATION_BOTTOM_BORDER_HEIGHT = 5;
    private static final int CONFIGURATION_BOTTOM_PADDING = 10;
    private static final int PANEL_WIDTH = SETTINGS_TEXTURE_WIDTH;
    private static final int CONFIGURATION_ROW_COUNT = 4;
    private static final int CONFIGURATION_ROW_HEIGHT = 30;
    private static final int PANEL_HEIGHT = CONFIGURATION_ROW_COUNT * CONFIGURATION_ROW_HEIGHT
            + 16 + CONFIGURATION_BOTTOM_PADDING;
    private static final int LABEL_X = 6;
    private static final int FIELD_X = LABEL_X;
    private static final int ORE_LABEL_Y = 18;
    private static final int ORE_FIELD_Y = 31;
    private static final int MODE_LABEL_Y = 48;
    private static final int MODE_Y = 61;
    private static final int MATCHING_LABEL_Y = 78;
    private static final int MATCHING_Y = 91;
    private static final int MIN_LABEL_Y = 108;
    private static final int MIN_FIELD_Y = 121;
    private static final int MODE_BUTTON_X = 6;
    private static final int MATCHING_BUTTON_X = 6;
    private static final int MODE_BUTTON_SIZE = 16;
    private static final int BUTTON_STATE_TEXTURE_WIDTH = MODE_BUTTON_SIZE * 5;
    private static final int BUTTON_STATE_TEXTURE_HEIGHT = MODE_BUTTON_SIZE;
    private static final int PANEL_CONTROL_SIZE = 13;
    private static final int MIN_DECREASE_X = 18;
    private static final int MIN_FIELD_X = 35;
    private static final int MIN_FIELD_WIDTH = 57;
    private static final int MIN_INCREASE_X = 96;
    private static final int MIN_FIELD_TEXT_PADDING = 4;
    private static final int FIELD_TEXT_PADDING = 4;
    private static final int FIELD_TEXT_COLOR = 0xFFFFFF;
    private static final int FIELD_BACKGROUND_COLOR = 0xFF8B8B8B;

    private final MEOreDictionaryInputBus bus;
    private int panelWidth = PANEL_WIDTH;
    private GuiTextField oreField;

    public GuiMEOreDictionaryInputBus(InventoryPlayer inventoryPlayer, MEOreDictionaryInputBus bus) {
        super(new ContainerMEOreDictionaryInputBus(inventoryPlayer, bus), bus.getPos(), bus::getMinStackSize, TEXTURE);
        this.bus = bus;
    }

    static boolean isPreviewSlot(int slotNumber) {
        return slotNumber >= 0 && slotNumber < INTERNAL_SLOT_COUNT;
    }

    static boolean isHoverableSlot(int slotNumber) {
        return !isPreviewSlot(slotNumber);
    }

    static boolean isDisplayTooltipSlot(int slotNumber) {
        return isPreviewSlot(slotNumber);
    }

    static boolean shouldPrioritizeSettingsPanelClick(boolean settingsPanelOpen, boolean clickInsideSettingsPanel) {
        return settingsPanelOpen && clickInsideSettingsPanel;
    }

    static boolean shouldSubmitBeforeChangingFieldFocus(boolean otherFieldFocused) {
        return otherFieldFocused;
    }

    static ResourceLocation configurationTexture() {
        return SETTINGS_TEXTURE;
    }

    static int configurationPanelWidth() {
        return PANEL_WIDTH;
    }

    static int configurationPanelWidthForContent(int contentWidth, int availableWidth) {
        int desiredWidth = Math.max(PANEL_WIDTH, contentWidth + LABEL_X * 2);
        return Math.min(desiredWidth, Math.max(PANEL_WIDTH, availableWidth));
    }

    static int configurationTextureCoordinateWidth(int targetWidth) {
        return targetWidth;
    }

    static int configurationTextureCoordinateHeight(int targetHeight) {
        return targetHeight;
    }

    static int configurationTopBorderHeight() {
        return CONFIGURATION_TOP_BORDER_HEIGHT;
    }

    static int configurationBottomBorderHeight() {
        return CONFIGURATION_BOTTOM_BORDER_HEIGHT;
    }

    static int configurationBottomPadding() {
        return CONFIGURATION_BOTTOM_PADDING;
    }

    static int configurationMiddleTargetHeight(int panelHeight) {
        return panelHeight - CONFIGURATION_TOP_BORDER_HEIGHT - CONFIGURATION_BOTTOM_BORDER_HEIGHT;
    }

    static int configurationPanelHeight() {
        return PANEL_HEIGHT;
    }

    static int configurationRowCount() {
        return CONFIGURATION_ROW_COUNT;
    }

    static int configurationRowHeight() {
        return CONFIGURATION_ROW_HEIGHT;
    }

    static int buttonStateTextureWidth() {
        return BUTTON_STATE_TEXTURE_WIDTH;
    }

    static int buttonStateTextureHeight() {
        return BUTTON_STATE_TEXTURE_HEIGHT;
    }

    static int adjustMinimumStock(int current, int delta) {
        int normalized = Math.max(1, current);
        if (delta < 0) return Math.max(1, normalized - 1);
        if (delta > 0) return normalized == Integer.MAX_VALUE ? Integer.MAX_VALUE : normalized + 1;
        return normalized;
    }

    static int pullModeFrame(int mode) {
        switch (mode) {
            case MEOreDictionaryInputBus.PULL_BY_NAME:
                return 0;
            case MEOreDictionaryInputBus.PULL_BY_MOD:
                return 2;
            case MEOreDictionaryInputBus.PULL_BY_AMOUNT:
            default:
                return 1;
        }
    }

    static int matchingModeFrame(int mode) {
        return mode == MEOreDictionaryInputBus.MATCH_PREFIX ? 4 : 3;
    }

    static int nextPullMode(int mode) {
        switch (mode) {
            case MEOreDictionaryInputBus.PULL_BY_NAME:
                return MEOreDictionaryInputBus.PULL_BY_AMOUNT;
            case MEOreDictionaryInputBus.PULL_BY_AMOUNT:
                return MEOreDictionaryInputBus.PULL_BY_MOD;
            case MEOreDictionaryInputBus.PULL_BY_MOD:
            default:
                return MEOreDictionaryInputBus.PULL_BY_NAME;
        }
    }

    static int nextMatchingMode(int mode) {
        return mode == MEOreDictionaryInputBus.MATCH_EXACT
                ? MEOreDictionaryInputBus.MATCH_PREFIX
                : MEOreDictionaryInputBus.MATCH_EXACT;
    }

    private static boolean isInBounds(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String modeTooltipKey(int mode) {
        switch (mode) {
            case MEOreDictionaryInputBus.PULL_BY_NAME:
                return "gui.mmce_more_bus.me_ore_dictionary_input_bus.mode.name.tooltip";
            case MEOreDictionaryInputBus.PULL_BY_MOD:
                return "gui.mmce_more_bus.me_ore_dictionary_input_bus.mode.mod.tooltip";
            case MEOreDictionaryInputBus.PULL_BY_AMOUNT:
            default:
                return "gui.mmce_more_bus.me_ore_dictionary_input_bus.mode.amount.tooltip";
        }
    }

    private static String matchingTooltipKey(int mode) {
        return mode == MEOreDictionaryInputBus.MATCH_PREFIX
                ? "gui.mmce_more_bus.me_ore_dictionary_input_bus.matching.prefix.tooltip"
                : "gui.mmce_more_bus.me_ore_dictionary_input_bus.matching.exact.tooltip";
    }

    @Override
    public void initGui() {
        super.initGui();
        panelWidth = PANEL_WIDTH;
        oreField = new GuiTextField(
                0,
                fontRenderer,
                settingsPanelX() + FIELD_X + FIELD_TEXT_PADDING,
                settingsPanelY() + ORE_FIELD_Y + 1,
                fieldWidth() - FIELD_TEXT_PADDING * 2,
                8
        );
        oreField.setMaxStringLength(128);
        oreField.setEnableBackgroundDrawing(false);
        oreField.setTextColor(FIELD_TEXT_COLOR);
        oreField.setText(bus.getOreDictionaryName());
        updatePanelWidth();
    }

    @Override
    public void drawFG(int guiLeft, int guiTop, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.mmce_more_bus.me_ore_dictionary_input_bus.title"), 8, 8, 0x404040);
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
        if (isSettingsPanelOpen()) updatePanelWidth();
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDisplayTooltip(mouseX, mouseY);
        if (isSettingsPanelOpen()) drawSettingsPanel(mouseX, mouseY);
        if (isSettingsPanelOpen() && isInBounds(mouseX, mouseY, settingsPanelX() + MODE_BUTTON_X,
                settingsPanelY() + MODE_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            drawIsolatedHoveringText(Collections.singletonList(I18n.format(modeTooltipKey(bus.getPullMode()))), mouseX, mouseY);
        } else if (isSettingsPanelOpen() && isInBounds(mouseX, mouseY, settingsPanelX() + MATCHING_BUTTON_X,
                settingsPanelY() + MATCHING_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            drawIsolatedHoveringText(Collections.singletonList(I18n.format(matchingTooltipKey(bus.getMatchingMode()))), mouseX, mouseY);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (oreField != null) oreField.updateCursorCounter();
    }

    private void drawDisplayTooltip(int mouseX, int mouseY) {
        Slot slot = findDisplaySlotAt(mouseX, mouseY);
        if (slot == null || slot.getStack().isEmpty()) return;

        List<String> tooltip = new ArrayList<>(getItemToolTip(slot.getStack()));
        if ((slot.slotNumber & 1) == 1) {
            tooltip = appendStoredAmount(tooltip, bus.getVirtualAmount(slot.slotNumber / 2));
        }
        drawIsolatedHoveringText(tooltip, mouseX, mouseY);
    }

    private Slot findDisplaySlotAt(int mouseX, int mouseY) {
        for (Slot slot : inventorySlots.inventorySlots) {
            if (isDisplayTooltipSlot(slot.slotNumber)
                    && slot.isEnabled()
                    && isPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    protected void drawSettingsPanel(int mouseX, int mouseY) {
        if (oreField == null || minimumStockField() == null) return;
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawSegmentedSettingsPanelBackground();
        drawSettingsLabel("gui.mmce_more_bus.me_ore_dictionary_input_bus.ore_dictionary", ORE_LABEL_Y);
        drawSettingsLabel("gui.mmce_more_bus.me_ore_dictionary_input_bus.mode", MODE_LABEL_Y);
        drawSettingsLabel("gui.mmce_more_bus.me_ore_dictionary_input_bus.matching", MATCHING_LABEL_Y);
        drawSettingsLabel("gui.mmce_more_bus.me_ore_dictionary_input_bus.minimum_stock", MIN_LABEL_Y);
        drawFieldBackground(ORE_FIELD_Y);
        oreField.drawTextBox();
        drawMinimumStockControls();
        drawStateButton(settingsPanelX() + MODE_BUTTON_X, settingsPanelY() + MODE_Y, pullModeFrame(bus.getPullMode()));
        drawStateButton(settingsPanelX() + MATCHING_BUTTON_X, settingsPanelY() + MATCHING_Y,
                matchingModeFrame(bus.getMatchingMode()));
    }

    private void drawSettingsLabel(String translationKey, int y) {
        String label = I18n.format(translationKey);
        if (fontRenderer.getStringWidth(label) > labelMaxWidth()) {
            label = fontRenderer.trimStringToWidth(label, labelMaxWidth() - fontRenderer.getStringWidth("...")) + "...";
        }
        fontRenderer.drawString(label, settingsPanelX() + LABEL_X, settingsPanelY() + y, 0x404040);
    }

    private void drawFieldBackground(int y) {
        drawRect(settingsPanelX() + FIELD_X - 1, settingsPanelY() + y - 2,
                settingsPanelX() + FIELD_X + fieldWidth() + 1, settingsPanelY() + y + 11, FIELD_BACKGROUND_COLOR);
    }

    private void drawStateButton(int x, int y, int frame) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BUTTON_STATE_TEXTURE);
        drawModalRectWithCustomSizedTexture(
                x,
                y,
                frame * MODE_BUTTON_SIZE,
                0,
                MODE_BUTTON_SIZE,
                MODE_BUTTON_SIZE,
                BUTTON_STATE_TEXTURE_WIDTH,
                BUTTON_STATE_TEXTURE_HEIGHT
        );
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (shouldPrioritizeSettingsPanelClick(
                isSettingsPanelOpen(),
                isInBounds(mouseX, mouseY, settingsPanelX(), settingsPanelY(), settingsPanelWidth(), settingsPanelHeight())
        )) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        Slot slot = super.getSlotAtPosition(mouseX, mouseY);
        if (slot != null && isPreviewSlot(slot.slotNumber)) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected boolean handleSettingsPanelClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isInBounds(mouseX, mouseY, settingsPanelX() + MODE_BUTTON_X,
                settingsPanelY() + MODE_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            playConfigurationButtonClickSound();
            setMode(nextPullMode(bus.getPullMode()));
            return true;
        }
        if (mouseButton == 0 && isInBounds(mouseX, mouseY, settingsPanelX() + MATCHING_BUTTON_X,
                settingsPanelY() + MATCHING_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            playConfigurationButtonClickSound();
            setMatchingMode(nextMatchingMode(bus.getMatchingMode()));
            return true;
        }
        if (isInBounds(mouseX, mouseY, settingsPanelX() + FIELD_X, settingsPanelY() + ORE_FIELD_Y - 2,
                fieldWidth(), 14)) {
            if (shouldSubmitBeforeChangingFieldFocus(minimumStockField().isFocused())) {
                submitConfiguration();
            }
            oreField.mouseClicked(mouseX, mouseY, mouseButton);
            minimumStockField().setFocused(false);
            return true;
        }
        if (isInBounds(mouseX, mouseY, settingsPanelX() + MIN_FIELD_X, settingsPanelY() + MIN_FIELD_Y,
                MIN_FIELD_WIDTH, PANEL_CONTROL_SIZE)) {
            if (shouldSubmitBeforeChangingFieldFocus(oreField.isFocused())) {
                submitConfiguration();
            }
            oreField.setFocused(false);
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isSettingsPanelOpen() && oreField != null && oreField.isFocused()) {
            if (keyCode == 28 || keyCode == 156) {
                submitConfiguration();
                oreField.setFocused(false);
                return;
            }
            if (oreField.textboxKeyTyped(typedChar, keyCode)) return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected int settingsPanelWidth() {
        return panelWidth;
    }

    @Override
    protected int settingsPanelHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    protected int minimumStockControlY() {
        return MIN_FIELD_Y;
    }

    @Override
    protected int minimumStockTextY() {
        return MIN_FIELD_Y + 3;
    }

    @Override
    protected int minimumStockDecreaseX() {
        return MIN_DECREASE_X;
    }

    @Override
    protected int minimumStockFieldX() {
        return MIN_FIELD_X;
    }

    @Override
    protected int minimumStockFieldWidth() {
        return MIN_FIELD_WIDTH;
    }

    @Override
    protected int minimumStockTextPadding() {
        return MIN_FIELD_TEXT_PADDING;
    }

    @Override
    protected int minimumStockIncreaseX() {
        return MIN_INCREASE_X;
    }

    @Override
    protected String settingsButtonTooltipKey() {
        return "gui.mmce_more_bus.me_ore_dictionary_input_bus.configure";
    }

    @Override
    protected void onSettingsPanelOpened() {
        if (oreField == null) return;
        oreField.setText(bus.getOreDictionaryName());
        oreField.setFocused(false);
        updatePanelWidth();
    }

    @Override
    protected void onSettingsPanelClickedOutside() {
        submitConfiguration();
        oreField.setFocused(false);
        minimumStockField().setFocused(false);
    }

    @Override
    protected void onSettingsPanelPositionChanged() {
        relocateOreField();
    }

    @Override
    protected void onMinimumStockChanged(int value) {
        submitConfiguration();
    }

    private void setMode(int mode) {
        bus.setPullMode(mode);
        submitConfiguration();
    }

    private void setMatchingMode(int value) {
        bus.setMatchingMode(value);
        submitConfiguration();
    }

    private void submitConfiguration() {
        if (oreField == null || minimumStockField() == null) return;
        int minimum = parseMinimumStock();
        minimumStockField().setText(Integer.toString(minimum));
        MEItemInventoryNetwork.CHANNEL.sendToServer(new MEItemInventoryNetwork.SetOreDictionaryConfigMessage(
                bus.getPos(), oreField.getText(), bus.getPullMode(), minimum, bus.getMatchingMode()));
    }

    private int parseMinimumStock() {
        try {
            return Math.max(1, Integer.parseInt(minimumStockField().getText()));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void relocateOreField() {
        if (oreField == null) return;
        oreField.x = settingsPanelX() + FIELD_X + FIELD_TEXT_PADDING;
        oreField.y = settingsPanelY() + ORE_FIELD_Y + 1;
        oreField.width = fieldWidth() - FIELD_TEXT_PADDING * 2;
    }

    private void updatePanelWidth() {
        if (oreField == null) return;
        int updatedWidth = configurationPanelWidthForContent(fontRenderer.getStringWidth(oreField.getText()), width);
        if (updatedWidth == panelWidth) return;
        panelWidth = updatedWidth;
        updateSettingsPanelLayout();
    }

    private int labelMaxWidth() {
        return panelWidth - LABEL_X * 2;
    }

    private int fieldWidth() {
        return panelWidth - LABEL_X * 2;
    }

    @Override
    public Slot getSlotAtPosition(int mouseX, int mouseY) {
        Slot slot = super.getSlotAtPosition(mouseX, mouseY);
        return slot != null && isPreviewSlot(slot.slotNumber) ? null : slot;
    }

    @Override
    public boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        if (slot == null || !isHoverableSlot(slot.slotNumber)) return false;
        return super.isMouseOverSlot(slot, mouseX, mouseY);
    }
}
