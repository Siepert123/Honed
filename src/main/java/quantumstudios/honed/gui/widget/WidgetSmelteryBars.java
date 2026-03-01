package quantumstudios.honed.gui.widget;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import quantumstudios.honed.config.HonedConfig;
import quantumstudios.honed.te.TileEntitySmeltery;

public class WidgetSmelteryBars<W extends WidgetSmelteryBars<W>> extends Widget<W> {
    private final TileEntitySmeltery smeltery;

    private static final int BAR_H = 8;
    private static final int FRAME = 0xFF2A2A34;
    private static final int BAR_BG = 0xFF151520;
    private static final int LABEL_COL = 0xFFBBBBCC;
    private static final int VALUE_COL = 0xFFDDDDDD;

    public WidgetSmelteryBars(TileEntitySmeltery smeltery) {
        this.smeltery = smeltery;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int w = getArea().width;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        float maxTemp = (float) HonedConfig.smelteryMaxTemp;

        // Temperature section
        font.drawStringWithShadow("Temperature", 0, 0, LABEL_COL);
        int tempY = 12;
        float temp = smeltery.getTemperature();
        float tempFrac = maxTemp > 0 ? Math.min(1f, temp / maxTemp) : 0;

        drawBar(0, tempY, w, BAR_H, tempFrac, true);

        String tempText = String.format("%.0f\u00b0C / %.0f\u00b0C", temp, maxTemp);
        font.drawStringWithShadow(tempText, w - font.getStringWidth(tempText), tempY + BAR_H + 2, VALUE_COL);

        // Fuel section
        int fuelLabelY = tempY + BAR_H + 16;
        font.drawStringWithShadow("Fuel", 0, fuelLabelY, LABEL_COL);
        int fuelY = fuelLabelY + 12;
        float fuelFrac = smeltery.getCurrentBurnTime() > 0
                ? (float) smeltery.getBurnTimeRemaining() / smeltery.getCurrentBurnTime() : 0f;

        drawBar(0, fuelY, w, BAR_H, fuelFrac, false);

        String fuelText = fuelFrac > 0 ? String.format("%.0f%%", fuelFrac * 100) : "Empty";
        font.drawStringWithShadow(fuelText, w - font.getStringWidth(fuelText), fuelY + BAR_H + 2, VALUE_COL);
    }

    private void drawBar(int x, int y, int w, int h, float frac, boolean isTemp) {
        GuiDraw.drawRect(x, y, w, h, FRAME);
        GuiDraw.drawRect(x + 1, y + 1, w - 2, h - 2, BAR_BG);
        if (frac > 0) {
            int fillW = Math.max(1, (int) (frac * (w - 2)));
            int col = isTemp ? heatGradient(frac) : fuelGradient(frac);
            GuiDraw.drawRect(x + 1, y + 1, fillW, h - 2, col);
            // Shimmer highlight on top pixel row
            int shimmer = isTemp ? heatShimmer(frac) : 0x40FFAA33;
            GuiDraw.drawRect(x + 1, y + 1, fillW, 1, shimmer);
        }
    }

    private static int heatGradient(float t) {
        if (t < 0.25f) return lerp(0xFF334488, 0xFFCC6622, t / 0.25f);
        if (t < 0.55f) return lerp(0xFFCC6622, 0xFFDD3333, (t - 0.25f) / 0.30f);
        if (t < 0.80f) return lerp(0xFFDD3333, 0xFFFF9922, (t - 0.55f) / 0.25f);
        return lerp(0xFFFF9922, 0xFFFFDD88, (t - 0.80f) / 0.20f);
    }

    private static int heatShimmer(float t) {
        if (t < 0.3f) return 0x303355AA;
        if (t < 0.6f) return 0x40CC7722;
        return 0x50FFBB44;
    }

    private static int fuelGradient(float t) {
        return lerp(0xFF663311, 0xFFFFAA22, t);
    }

    private static int lerp(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((int) (a1 + (a2 - a1) * t) << 24)
                | ((int) (r1 + (r2 - r1) * t) << 16)
                | ((int) (g1 + (g2 - g1) * t) << 8)
                | (int) (b1 + (b2 - b1) * t);
    }

    @Override
    public int getDefaultHeight() {
        return 68;
    }
}
