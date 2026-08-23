package yalter.mousetweaks.api;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

/**
 * Compatibility API implemented by AE2 GUI classes for Mouse Tweaks.
 *
 * <p>AE2 exposes this interface on its GUI base class even when Mouse Tweaks
 * itself is absent. Keeping the API locally lets that optional linkage resolve
 * without requiring NAE2.</p>
 */
public interface IMTModGuiContainer2 {
    boolean MT_isMouseTweaksDisabled();

    boolean MT_isWheelTweakDisabled();

    Container MT_getContainer();

    Slot MT_getSlotUnderMouse();

    boolean MT_isCraftingOutput(Slot slot);

    boolean MT_isIgnored(Slot slot);

    boolean MT_disableRMBDraggingFunctionality();
}
