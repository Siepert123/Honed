package quantumstudios.honed.item.color;

import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import quantumstudios.honed.config.HonedConfig;
import quantumstudios.honed.tool.ToolNBT;
@SideOnly(Side.CLIENT)
public class HeatItemColor implements IItemColor {

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return -1;
        float temp = ToolNBT.getTemperature(stack);
        if (temp <= 30f) return -1; // below 30 K above zero = no visible tint
        float maxTemp = (float) HonedConfig.smelteryMaxTemp;
        float ratio = Math.min(temp / maxTemp, 1f);
        return heatTint(ratio);
    }
    public static int heatTint(float ratio) {
        int r, g, b;
        if (ratio < 0.05f) {
            // white → barely pink
            float p = ratio / 0.05f;
            r = 255;
            g = (int) (255 - 55 * p);   // 255 → 200
            b = (int) (255 - 55 * p);   // 255 → 200
        } else if (ratio < 0.15f) {
            // barely pink → light rose
            float p = (ratio - 0.05f) / 0.10f;
            r = 255;
            g = (int) (200 - 70 * p);   // 200 → 130
            b = (int) (200 - 70 * p);   // 200 → 130
        } else if (ratio < 0.30f) {
            // light rose → dark red
            float p = (ratio - 0.15f) / 0.15f;
            r = (int) (255 - 105 * p);  // 255 → 150
            g = (int) (130 - 130 * p);  // 130 → 0
            b = 0;
        } else if (ratio < 0.50f) {
            // dark red → full red
            float p = (ratio - 0.30f) / 0.20f;
            r = (int) (150 + 105 * p);  // 150 → 255
            g = 0; b = 0;
        } else if (ratio < 0.70f) {
            // red → orange
            float p = (ratio - 0.50f) / 0.20f;
            r = 255;
            g = (int) (165 * p);        // 0 → 165
            b = 0;
        } else if (ratio < 0.85f) {
            // orange → yellow
            float p = (ratio - 0.70f) / 0.15f;
            r = 255;
            g = (int) (165 + 90 * p);   // 165 → 255
            b = 0;
        } else {
            // yellow → white
            float p = (ratio - 0.85f) / 0.15f;
            r = 255; g = 255;
            b = (int) (255 * p);        // 0 → 255
        }
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
