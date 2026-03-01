package quantumstudios.honed.util;

import net.minecraft.util.text.TextFormatting;

public final class ColorHelper {
    private ColorHelper() {}

    public static String rgbTag(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    public static int parseColor(String str) {
        if (str == null || str.isEmpty()) return 0xFFFFFF;
        str = str.trim();
        if (str.startsWith("#"))  str = str.substring(1);
        if (str.startsWith("0x") || str.startsWith("0X")) str = str.substring(2);
        try {
            return (int) (Long.parseLong(str, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            try { return Integer.parseInt(str) & 0xFFFFFF; }
            catch (NumberFormatException e2) { return 0xFFFFFF; }
        }
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static TextFormatting nearestFormatting(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        float lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
        if (lum > 0.90f) return TextFormatting.WHITE;
        if (lum < 0.15f) return TextFormatting.DARK_GRAY;
        if (r > g * 1.5 && r > b * 1.5) return lum > 0.5f ? TextFormatting.RED : TextFormatting.DARK_RED;
        if (g > r * 1.5 && g > b * 1.5) return lum > 0.5f ? TextFormatting.GREEN : TextFormatting.DARK_GREEN;
        if (b > r * 1.5 && b > g * 1.5) return lum > 0.5f ? TextFormatting.BLUE : TextFormatting.DARK_BLUE;
        if (r > b * 1.3 && g > b * 1.3) return TextFormatting.YELLOW;
        if (r > g * 1.3 && b > g * 1.3) return TextFormatting.LIGHT_PURPLE;
        if (g > r * 1.3 && b > r * 1.3) return TextFormatting.AQUA;
        return lum > 0.5f ? TextFormatting.GRAY : TextFormatting.DARK_GRAY;
    }

    public static int blend(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        return (clamp(Math.round(r1 + (r2 - r1) * ratio)) << 16)
             | (clamp(Math.round(g1 + (g2 - g1) * ratio)) << 8)
             |  clamp(Math.round(b1 + (b2 - b1) * ratio));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
