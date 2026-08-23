package github.starfall063.mmce_more_bus.gui;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.widgets.GuiCustomSlot;
import appeng.client.render.StackSizeRenderer;
import appeng.fluids.client.gui.widgets.GuiFluidSlot;
import appeng.fluids.client.render.FluidStackSizeRenderer;
import appeng.fluids.util.AEFluidStack;
import appeng.util.item.AEItemStack;
import com.mekeng.github.client.render.GasStackSizeRenderer;
import com.mekeng.github.client.slots.SlotGas;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import github.kasuminova.mmce.client.gui.AEBaseGuiContainerDynamic;
import github.kasuminova.mmce.client.gui.widget.Button4State;
import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.starfall063.mmce_more_bus.Tags;
import github.starfall063.mmce_more_bus.module.mmce.me.MEItemInventoryNetwork;
import mekanism.api.gas.GasStack;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;

abstract class GuiMEInventoryInputBusBase extends AEBaseGuiContainerDynamic implements HeiExtraAreaProvider {
    private static final StackSizeRenderer ITEM_AMOUNT_RENDERER = new StackSizeRenderer();
    private static final FluidStackSizeRenderer FLUID_AMOUNT_RENDERER = new FluidStackSizeRenderer();
    private static final GasStackSizeRenderer GAS_AMOUNT_RENDERER = new GasStackSizeRenderer();
    private static final int GAS_AMOUNT_OFFSET_X = 0;
    private static final int GAS_AMOUNT_OFFSET_Y = 0;
    private static final ResourceLocation SETTINGS_TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/movable_configuration.png"
    );
    private static final int SETTINGS_BUTTON_X = 144;
    private static final int SETTINGS_BUTTON_Y = 2;
    private static final int SETTINGS_BUTTON_SIZE = 13;
    private static final int SETTINGS_TEXTURE_WIDTH = 127;
    private static final int SETTINGS_TEXTURE_HEIGHT = 54;
    private static final int SEGMENTED_PANEL_BORDER_HEIGHT = 5;
    private static final int SEGMENTED_PANEL_MIDDLE_TEXTURE_Y = SEGMENTED_PANEL_BORDER_HEIGHT;
    private static final int SEGMENTED_PANEL_MIDDLE_TEXTURE_HEIGHT = SETTINGS_TEXTURE_HEIGHT - 10;
    private static final int SEGMENTED_PANEL_BOTTOM_TEXTURE_Y = SETTINGS_TEXTURE_HEIGHT - SEGMENTED_PANEL_BORDER_HEIGHT;
    private static final int PANEL_WIDTH = SETTINGS_TEXTURE_WIDTH;
    private static final int SETTINGS_LABEL_MAX_WIDTH = PANEL_WIDTH - SETTINGS_LABEL_X * 2;
    private static final int PANEL_HEIGHT = SETTINGS_TEXTURE_HEIGHT;
    private static final int PANEL_DRAG_HEIGHT = 16;
    private static final int PANEL_CONTROL_SIZE = 13;
    private static final int SETTINGS_LABEL_Y = 18;
    private static final int SETTINGS_ROW_Y = 32;
    private static final int SETTINGS_TEXT_Y = SETTINGS_ROW_Y + 3;
    private static final int SETTINGS_LABEL_X = 6;
    private static final int DECREASE_BUTTON_X = 18;
    private static final int MIN_STACK_SIZE_FIELD_X = 35;
    private static final int MIN_STACK_SIZE_FIELD_WIDTH = 57;
    private static final int MIN_STACK_SIZE_TEXT_PADDING = 4;
    private static final int MIN_STACK_SIZE_TEXT_COLOR = 0xFFFFFF;
    private static final int MIN_STACK_SIZE_FIELD_BACKGROUND_COLOR = 0xFF8B8B8B;
    private static final int INCREASE_BUTTON_X = 96;
    private static int rememberedPanelX = Integer.MIN_VALUE;
    private static int rememberedPanelY;
    private final CachedFluidPreviewSlot fluidPreviewSlot = new CachedFluidPreviewSlot();
    private final CachedGasPreviewSlot gasPreviewSlot = new CachedGasPreviewSlot();
    private final BlockPos position;
    private final IntSupplier minimumStock;
    private final ResourceLocation mainTexture;
    private boolean settingsOpen;
    private boolean draggingPanel;
    private int panelX = Integer.MIN_VALUE;
    private int panelY;
    private int dragOffsetX;
    private int dragOffsetY;
    private GuiTextField minimumStockField;

    protected GuiMEInventoryInputBusBase(
            Container container,
            BlockPos position,
            IntSupplier minimumStock,
            ResourceLocation mainTexture
    ) {
        super(container);
        this.position = position;
        this.minimumStock = minimumStock;
        this.mainTexture = mainTexture;
        xSize = 176;
        ySize = 206;
        widgetController = new WidgetController(WidgetGui.of(this, xSize, ySize, guiLeft, guiTop));
        widgetController.addWidget(createSettingsButton());
    }

    static String sharedConfigurationTranslationKey(String suffix) {
        return "gui.mmce_more_bus.me_input_bus." + suffix;
    }

    static String storedAmountText(String amount) {
        return "\u00a77" + I18n.format("gui.mmce_more_bus.me_inventory_bus.stored_amount", amount);
    }

    static String storedAmountText(long amount) {
        return storedAmountText(formatStoredAmount(amount));
    }

    static String storedFluidAmountText(long amount) {
        return storedAmountText(formatStoredAmount(amount) + " mB");
    }

    static List<String> appendStoredAmount(List<String> tooltip, long amount) {
        return appendTooltipLine(tooltip, storedAmountText(amount));
    }

    static List<String> appendStoredFluidAmount(List<String> tooltip, long amount) {
        return appendTooltipLine(tooltip, storedFluidAmountText(amount));
    }

    private static List<String> appendTooltipLine(List<String> tooltip, String line) {
        List<String> result = new ArrayList<>(tooltip);
        result.add(line);
        return result;
    }

    private static String formatStoredAmount(long amount) {
        return String.format(Locale.ROOT, "%,d", Math.max(0L, amount));
    }

    /**
     * AE2's fluid and gas slots can leave their tint and lighting state active.
     * Restore the baseline expected by the following item slot.
     */
    protected static void restoreNativeResourceRenderState() {
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(org.lwjgl.opengl.GL11.GL_GREATER, 0.1F);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static void renderItemAmount(FontRenderer fontRenderer, ItemStack stack, long amount, int slotX, int slotY) {
        if (stack == null || stack.isEmpty()) return;
        if (amount <= 0L) return;
        IAEItemStack aeStack = AEItemStack.fromItemStack(stack);
        if (aeStack == null) return;
        aeStack.setStackSize(amount);
        ITEM_AMOUNT_RENDERER.renderStackSize(fontRenderer, aeStack, slotX, slotY);
    }

    static void renderFluidAmount(FontRenderer fontRenderer, FluidStack stack, long amount, int slotX, int slotY) {
        if (stack == null) return;
        if (amount <= 0L) return;
        IAEFluidStack aeStack = AEFluidStack.fromFluidStack(stack);
        if (aeStack == null) return;
        aeStack.setStackSize(amount);
        FLUID_AMOUNT_RENDERER.renderStackSize(fontRenderer, aeStack, slotX, slotY);
    }

    static void renderGasAmount(FontRenderer fontRenderer, GasStack stack, long amount, int slotX, int slotY) {
        if (stack == null) return;
        if (amount <= 0L) return;
        IAEGasStack aeStack = AEGasStack.of(stack);
        if (aeStack == null) return;
        aeStack.setStackSize(amount);
        GAS_AMOUNT_RENDERER.renderStackSize(
                fontRenderer,
                aeStack,
                slotX + GAS_AMOUNT_OFFSET_X,
                slotY + GAS_AMOUNT_OFFSET_Y
        );
    }

    static SoundEvent configurationButtonClickSound() {
        return SoundEvents.UI_BUTTON_CLICK;
    }

    private static boolean isInBounds(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (rememberedPanelX == Integer.MIN_VALUE) {
            panelX = Math.min(width - settingsPanelWidth(), guiLeft + xSize + 4);
            panelY = Math.max(0, guiTop + 2);
        } else {
            panelX = rememberedPanelX;
            panelY = rememberedPanelY;
        }
        clampPanelPosition();
        minimumStockField = new GuiTextField(
                0,
                fontRenderer,
                panelX + minimumStockFieldX() + minimumStockTextPadding(),
                panelY + minimumStockTextY(),
                minimumStockFieldWidth() - minimumStockTextPadding() * 2,
                8
        );
        minimumStockField.setMaxStringLength(10);
        minimumStockField.setValidator(value -> value.isEmpty() || value.matches("\\d{1,10}"));
        minimumStockField.setEnableBackgroundDrawing(false);
        minimumStockField.setTextColor(MIN_STACK_SIZE_TEXT_COLOR);
        minimumStockField.setText(Integer.toString(minimumStock.getAsInt()));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (minimumStockField != null) minimumStockField.updateCursorCounter();
    }

    protected void drawSettingsPanel(int mouseX, int mouseY) {
        if (!settingsOpen || minimumStockField == null) return;

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawSegmentedSettingsPanelBackground();

        String label = I18n.format(sharedConfigurationTranslationKey("minimum_stock"));
        fontRenderer.drawString(trimLabel(label), panelX + SETTINGS_LABEL_X, panelY + SETTINGS_LABEL_Y, 0x404040);
        drawMinimumStockControls();

        if (fontRenderer.getStringWidth(label) > minimumStockLabelMaxWidth() && isInBounds(
                mouseX,
                mouseY,
                panelX + SETTINGS_LABEL_X,
                panelY + SETTINGS_LABEL_Y,
                minimumStockLabelMaxWidth(),
                fontRenderer.FONT_HEIGHT
        )) {
            drawIsolatedHoveringText(Collections.singletonList(label), mouseX, mouseY);
        }
        if (isInBounds(
                mouseX,
                mouseY,
                panelX + minimumStockDecreaseX(),
                panelY + minimumStockControlY(),
                PANEL_CONTROL_SIZE,
                PANEL_CONTROL_SIZE
        )) {
            drawIsolatedHoveringText(
                    Collections.singletonList(I18n.format(sharedConfigurationTranslationKey("decrease"))),
                    mouseX,
                    mouseY
            );
        }
        if (isInBounds(
                mouseX,
                mouseY,
                panelX + minimumStockIncreaseX(),
                panelY + minimumStockControlY(),
                PANEL_CONTROL_SIZE,
                PANEL_CONTROL_SIZE
        )) {
            drawIsolatedHoveringText(
                    Collections.singletonList(I18n.format(sharedConfigurationTranslationKey("increase"))),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (settingsOpen && minimumStockField != null) {
            boolean insidePanel = isInBounds(
                    mouseX, mouseY, panelX, panelY, settingsPanelWidth(), settingsPanelHeight()
            );
            if (isInBounds(mouseX, mouseY, panelX, panelY, settingsPanelWidth(), PANEL_DRAG_HEIGHT)) {
                draggingPanel = true;
                dragOffsetX = mouseX - panelX;
                dragOffsetY = mouseY - panelY;
                return;
            }
            if (handleSettingsPanelClick(mouseX, mouseY, mouseButton)) return;
            if (isInBounds(
                    mouseX,
                    mouseY,
                    panelX + minimumStockDecreaseX(),
                    panelY + minimumStockControlY(),
                    PANEL_CONTROL_SIZE,
                    PANEL_CONTROL_SIZE
            )) {
                playConfigurationButtonClickSound();
                changeMinimumStock(-1);
                return;
            }
            if (isInBounds(
                    mouseX,
                    mouseY,
                    panelX + minimumStockIncreaseX(),
                    panelY + minimumStockControlY(),
                    PANEL_CONTROL_SIZE,
                    PANEL_CONTROL_SIZE
            )) {
                playConfigurationButtonClickSound();
                changeMinimumStock(1);
                return;
            }
            if (isInBounds(
                    mouseX,
                    mouseY,
                    panelX + minimumStockFieldX(),
                    panelY + minimumStockControlY(),
                    minimumStockFieldWidth(),
                    PANEL_CONTROL_SIZE
            )) {
                minimumStockField.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
            if (insidePanel) return;
            onSettingsPanelClickedOutside();
            if (minimumStockField.isFocused()) {
                submitMinimumStock();
                minimumStockField.setFocused(false);
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingPanel && clickedMouseButton == 0) {
            panelX = mouseX - dragOffsetX;
            panelY = mouseY - dragOffsetY;
            clampPanelPosition();
            relocateMinimumStockField();
            onSettingsPanelPositionChanged();
            rememberedPanelX = panelX;
            rememberedPanelY = panelY;
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && draggingPanel) {
            draggingPanel = false;
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (settingsOpen && minimumStockField != null && minimumStockField.isFocused()) {
            if (keyCode == 28 || keyCode == 156) {
                submitMinimumStock();
                minimumStockField.setFocused(false);
                return;
            }
            if (minimumStockField.textboxKeyTyped(typedChar, keyCode)) return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    public final List<Rectangle> getHeiExtraAreas() {
        if (!settingsOpen || panelX == Integer.MIN_VALUE) return Collections.emptyList();
        return Collections.singletonList(new Rectangle(panelX, panelY, settingsPanelWidth(), settingsPanelHeight()));
    }

    protected int settingsPanelWidth() {
        return PANEL_WIDTH;
    }

    protected int settingsPanelHeight() {
        return PANEL_HEIGHT;
    }

    protected int minimumStockLabelMaxWidth() {
        return settingsPanelWidth() - SETTINGS_LABEL_X * 2;
    }

    protected int minimumStockControlY() {
        return SETTINGS_ROW_Y;
    }

    protected int minimumStockTextY() {
        return SETTINGS_TEXT_Y;
    }

    protected int minimumStockDecreaseX() {
        return DECREASE_BUTTON_X;
    }

    protected int minimumStockFieldX() {
        return MIN_STACK_SIZE_FIELD_X;
    }

    protected int minimumStockFieldWidth() {
        return MIN_STACK_SIZE_FIELD_WIDTH;
    }

    protected int minimumStockTextPadding() {
        return MIN_STACK_SIZE_TEXT_PADDING;
    }

    protected int minimumStockIncreaseX() {
        return INCREASE_BUTTON_X;
    }

    protected String settingsButtonTooltipKey() {
        return sharedConfigurationTranslationKey("configure");
    }

    protected final void renderPreviewResource(PreviewResource resource, int x, int y) {
        if (resource == null || resource.amount <= 0L) return;

        switch (resource.type) {
            case ITEM:
                if (resource.item == null || resource.item.isEmpty()) return;
                renderItemPreview(resource.item, x, y);
                renderItemAmount(fontRenderer, resource.item, resource.amount, x, y);
                break;
            case FLUID:
                if (resource.fluid == null) return;
                renderFluidPreview(resource.fluid, x, y);
                renderFluidAmount(fontRenderer, resource.fluid, resource.amount, x + 1, y + 1);
                break;
            case GAS:
                if (resource.gas == null) return;
                renderGasPreview(resource.gas, x, y);
                renderGasAmount(fontRenderer, resource.gas, resource.amount, x + 1, y + 1);
                break;
            default:
                break;
        }
    }

    protected final void drawPreviewTooltip(PreviewResource resource, int mouseX, int mouseY) {
        if (resource == null || resource.amount <= 0L) return;

        List<String> tooltip;
        switch (resource.type) {
            case ITEM:
                if (resource.item == null || resource.item.isEmpty()) return;
                tooltip = new ArrayList<>(getItemToolTip(resource.item));
                break;
            case FLUID:
                if (resource.fluid == null) return;
                tooltip = new ArrayList<>(Collections.singletonList(resource.fluid.getLocalizedName()));
                break;
            case GAS:
                if (resource.gas == null || resource.gas.getGas() == null) return;
                tooltip = new ArrayList<>(Collections.singletonList(resource.gas.getGas().getLocalizedName()));
                break;
            default:
                return;
        }
        drawIsolatedHoveringText(
                resource.type == PreviewType.ITEM
                        ? appendStoredAmount(tooltip, resource.amount)
                        : appendStoredFluidAmount(tooltip, resource.amount),
                mouseX,
                mouseY
        );
    }

    /**
     * Tooltip rendering is allowed to change texture, blend, lighting, and
     * matrix state. Keep those changes local so custom item models cannot
     * affect the next slot or the next frame.
     */
    protected final void drawIsolatedHoveringText(List<String> tooltip, int mouseX, int mouseY) {
        int previousMatrixMode = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MATRIX_MODE);
        GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            drawHoveringText(tooltip, mouseX, mouseY);
        } finally {
            GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(previousMatrixMode);
            restoreNativeResourceRenderState();
        }
    }

    protected final void renderItemPreview(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;

        ItemStack displayed = stack.copy();
        displayed.setCount(1);
        float previousGuiZ = zLevel;
        float previousItemZ = itemRender.zLevel;
        int previousMatrixMode = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MATRIX_MODE);
        GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            zLevel = 100.0F;
            itemRender.zLevel = 100.0F;
            itemRender.renderItemAndEffectIntoGUI(displayed, x, y);
            itemRender.renderItemOverlayIntoGUI(fontRenderer, displayed, x, y, null);
        } finally {
            GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(previousMatrixMode);
            zLevel = previousGuiZ;
            itemRender.zLevel = previousItemZ;
            restoreNativeResourceRenderState();
        }
    }

    protected final void renderInputItemSlot(Slot slot, long amount) {
        if (slot == null || slot.getStack().isEmpty()) return;
        renderItemPreview(slot.getStack(), slot.xPos, slot.yPos);
        renderItemAmount(fontRenderer, slot.getStack(), amount, slot.xPos, slot.yPos);
    }

    protected final void renderFluidPreview(FluidStack stack, int x, int y) {
        if (stack == null) return;

        FluidStack displayed = stack.copy();
        displayed.amount = 1;
        fluidPreviewSlot.update(displayed, x + 1, y + 1);
        renderNativeSlot(fluidPreviewSlot, x + 1, y + 1);
    }

    protected final void renderGasPreview(GasStack stack, int x, int y) {
        if (stack == null) return;

        GasStack displayed = stack.copy();
        displayed.amount = 1;
        gasPreviewSlot.update(displayed, x + 1, y + 1);
        renderNativeSlot(gasPreviewSlot, x + 1, y + 1);
    }

    private void renderNativeSlot(
            GuiCustomSlot slot,
            int x,
            int y
    ) {
        try {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            slot.drawContent(mc, x, y, 0.0F);
        } finally {
            restoreNativeResourceRenderState();
        }
    }

    protected final void drawInheritedSlot(Slot slot) {
        int previousMatrixMode = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MATRIX_MODE);
        GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            super.drawSlot(slot);
        } finally {
            GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(previousMatrixMode);
            restoreNativeResourceRenderState();
        }
    }

    protected final void playConfigurationButtonClickSound() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                configurationButtonClickSound(), 1.0F
        ));
    }

    protected void onSettingsPanelOpened() {
    }

    protected boolean handleSettingsPanelClick(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    protected void onSettingsPanelClickedOutside() {
    }

    protected void onSettingsPanelPositionChanged() {
    }

    protected final boolean isSettingsPanelOpen() {
        return settingsOpen;
    }

    protected final int settingsPanelX() {
        return panelX;
    }

    protected final int settingsPanelY() {
        return panelY;
    }

    protected final GuiTextField minimumStockField() {
        return minimumStockField;
    }

    protected final void updateSettingsPanelLayout() {
        clampPanelPosition();
        relocateMinimumStockField();
        onSettingsPanelPositionChanged();
    }

    private Button4State createSettingsButton() {
        Button4State button = new Button4State();
        button.setMouseDownTexture(206, 0)
                .setHoveredTexture(191, 0)
                .setTexture(176, 0)
                .setTextureLocation(mainTexture)
                .setTooltipFunction(ignored -> Collections.singletonList(
                        I18n.format(settingsButtonTooltipKey())
                ))
                .setOnClickedListener(ignored -> {
                    playConfigurationButtonClickSound();
                    toggleSettings();
                })
                .setWidthHeight(SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE)
                .setAbsXY(SETTINGS_BUTTON_X, SETTINGS_BUTTON_Y);
        return button;
    }

    private void toggleSettings() {
        settingsOpen = !settingsOpen;
        if (settingsOpen && minimumStockField != null) {
            minimumStockField.setText(Integer.toString(minimumStock.getAsInt()));
            minimumStockField.setFocused(false);
            onSettingsPanelOpened();
        }
    }

    private String trimLabel(String label) {
        int maxWidth = minimumStockLabelMaxWidth();
        if (fontRenderer.getStringWidth(label) <= maxWidth) return label;
        return fontRenderer.trimStringToWidth(label, maxWidth - fontRenderer.getStringWidth("...")) + "...";
    }

    protected final void drawMinimumStockControls() {
        drawPanelButton(panelX + minimumStockDecreaseX(), panelY + minimumStockControlY(), "-");
        drawMinimumStockFieldBackground();
        drawPanelButton(panelX + minimumStockIncreaseX(), panelY + minimumStockControlY(), "+");
        minimumStockField.drawTextBox();
    }

    protected final void drawSegmentedSettingsPanelBackground() {
        int panelWidth = settingsPanelWidth();
        int panelHeight = settingsPanelHeight();
        mc.getTextureManager().bindTexture(SETTINGS_TEXTURE);
        drawModalRectWithCustomSizedTexture(
                panelX,
                panelY,
                0,
                0,
                panelWidth,
                SEGMENTED_PANEL_BORDER_HEIGHT,
                panelWidth,
                SETTINGS_TEXTURE_HEIGHT
        );

        int middleTargetHeight = panelHeight - SEGMENTED_PANEL_BORDER_HEIGHT * 2;
        GlStateManager.pushMatrix();
        GlStateManager.translate(panelX, panelY + SEGMENTED_PANEL_BORDER_HEIGHT, 0.0F);
        GlStateManager.scale(
                1.0F,
                (float) middleTargetHeight / SEGMENTED_PANEL_MIDDLE_TEXTURE_HEIGHT,
                1.0F
        );
        drawModalRectWithCustomSizedTexture(
                0,
                0,
                0,
                SEGMENTED_PANEL_MIDDLE_TEXTURE_Y,
                panelWidth,
                SEGMENTED_PANEL_MIDDLE_TEXTURE_HEIGHT,
                panelWidth,
                SETTINGS_TEXTURE_HEIGHT
        );
        GlStateManager.popMatrix();

        drawModalRectWithCustomSizedTexture(
                panelX,
                panelY + panelHeight - SEGMENTED_PANEL_BORDER_HEIGHT,
                0,
                SEGMENTED_PANEL_BOTTOM_TEXTURE_Y,
                panelWidth,
                SEGMENTED_PANEL_BORDER_HEIGHT,
                panelWidth,
                SETTINGS_TEXTURE_HEIGHT
        );
    }

    protected final void drawPanelButton(int x, int y, String label) {
        drawRect(x, y, x + PANEL_CONTROL_SIZE, y + PANEL_CONTROL_SIZE, 0xFF70808C);
        drawRect(x + 1, y + 1, x + PANEL_CONTROL_SIZE - 1, y + PANEL_CONTROL_SIZE - 1, 0xFFBBC3C9);
        fontRenderer.drawString(label, x + 4, y + 3, 0xFF404040);
    }

    private void drawMinimumStockFieldBackground() {
        int x = panelX + minimumStockFieldX();
        int y = panelY + minimumStockTextY();
        drawRect(x, y, x + minimumStockFieldWidth(), y + fontRenderer.FONT_HEIGHT, MIN_STACK_SIZE_FIELD_BACKGROUND_COLOR);
    }

    private void changeMinimumStock(int delta) {
        int current = parseMinimumStock();
        int updated = delta < 0
                ? Math.max(1, current - 1)
                : current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1;
        minimumStockField.setText(Integer.toString(updated));
        onMinimumStockChanged(updated);
    }

    private void submitMinimumStock() {
        int value = parseMinimumStock();
        minimumStockField.setText(Integer.toString(value));
        onMinimumStockChanged(value);
    }

    private int parseMinimumStock() {
        try {
            return Math.max(1, Integer.parseInt(minimumStockField.getText()));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    protected void onMinimumStockChanged(int value) {
        MEItemInventoryNetwork.CHANNEL.sendToServer(
                new MEItemInventoryNetwork.SetMinStackSizeMessage(position, value)
        );
    }

    private void relocateMinimumStockField() {
        minimumStockField.x = panelX + minimumStockFieldX() + minimumStockTextPadding();
        minimumStockField.y = panelY + minimumStockTextY();
    }

    private void clampPanelPosition() {
        panelX = Math.max(0, Math.min(panelX, width - settingsPanelWidth()));
        panelY = Math.max(0, Math.min(panelY, height - settingsPanelHeight()));
    }

    protected enum PreviewType {
        ITEM,
        FLUID,
        GAS
    }

    protected static final class PreviewResource {
        private final PreviewType type;
        private final ItemStack item;
        private final FluidStack fluid;
        private final GasStack gas;
        private final long amount;

        private PreviewResource(PreviewType type, ItemStack item, FluidStack fluid, GasStack gas, long amount) {
            this.type = type;
            this.item = item;
            this.fluid = fluid;
            this.gas = gas;
            this.amount = amount;
        }

        protected static PreviewResource item(ItemStack stack, long amount) {
            return new PreviewResource(PreviewType.ITEM, stack, null, null, amount);
        }

        protected static PreviewResource fluid(FluidStack stack, long amount) {
            return new PreviewResource(PreviewType.FLUID, null, stack, null, amount);
        }

        protected static PreviewResource gas(GasStack stack, long amount) {
            return new PreviewResource(PreviewType.GAS, null, null, stack, amount);
        }
    }

    private static final class CachedFluidPreviewSlot extends GuiFluidSlot {
        private FluidStack stack;
        private int renderX;
        private int renderY;

        private CachedFluidPreviewSlot() {
            super(null, 0, 0, 0, 0);
        }

        private void update(FluidStack stack, int x, int y) {
            this.stack = stack;
            renderX = x;
            renderY = y;
        }

        @Override
        public int xPos() {
            return renderX;
        }

        @Override
        public int yPos() {
            return renderY;
        }

        @Override
        public IAEFluidStack getFluidStack() {
            return stack == null ? null : AEFluidStack.fromFluidStack(stack);
        }

        @Override
        public void setFluidStack(IAEFluidStack ignored) {
        }
    }

    private static final class CachedGasPreviewSlot extends SlotGas {
        private GasStack stack;
        private int renderX;
        private int renderY;

        private CachedGasPreviewSlot() {
            super(null, 0, 0, 0, 0);
        }

        private void update(GasStack stack, int x, int y) {
            this.stack = stack;
            renderX = x;
            renderY = y;
        }

        @Override
        public int xPos() {
            return renderX;
        }

        @Override
        public int yPos() {
            return renderY;
        }

        @Override
        public GasStack getGasStack() {
            return stack;
        }

        @Override
        public void setGasStack(GasStack ignored) {
        }
    }
}
