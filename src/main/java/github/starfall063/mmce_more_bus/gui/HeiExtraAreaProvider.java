package github.starfall063.mmce_more_bus.gui;

import java.awt.*;
import java.util.List;

/**
 * Supplies GUI regions that must remain outside HEI's overlay area.
 */
public interface HeiExtraAreaProvider {
    List<Rectangle> getHeiExtraAreas();
}
