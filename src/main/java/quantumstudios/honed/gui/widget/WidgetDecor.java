package quantumstudios.honed.gui.widget;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

public class WidgetDecor<W extends WidgetDecor<W>> extends Widget<W> {
    private int bg = 0x30000000;
    private int border = 0x18FFFFFF;
    private int topAccent = 0;
    private boolean hasTopAccent = false;

    @SuppressWarnings("unchecked")
    public W colors(int background, int borderColor) {
        this.bg = background;
        this.border = borderColor;
        return (W) this;
    }

    @SuppressWarnings("unchecked")
    public W topAccent(int color) {
        this.topAccent = color;
        this.hasTopAccent = true;
        return (W) this;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int w = getArea().width;
        int h = getArea().height;
        GuiDraw.drawRect(0, 0, w, h, bg);
        // Border: top, left, bottom, right
        GuiDraw.drawRect(0, 0, w, 1, border);
        GuiDraw.drawRect(0, h - 1, w, 1, border);
        GuiDraw.drawRect(0, 1, 1, h - 2, border);
        GuiDraw.drawRect(w - 1, 1, 1, h - 2, border);
        if (hasTopAccent) {
            GuiDraw.drawRect(1, 1, w - 2, 1, topAccent);
        }
    }
}
