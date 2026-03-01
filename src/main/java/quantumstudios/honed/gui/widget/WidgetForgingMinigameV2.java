package quantumstudios.honed.gui.widget;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import quantumstudios.honed.te.TileEntityForgingAnvil;

public class WidgetForgingMinigameV2<W extends WidgetForgingMinigameV2<W>> extends Widget<W> {
    private final TileEntityForgingAnvil anvil;

    private static final int BAR_H = 16;
    private static final int STRIP_H = 6;
    private static final int GAP = 2;
    private static final int PAD = 2;
    private static final int PASSES = TileEntityForgingAnvil.TOTAL_PASSES;

    // Frame
    private static final int F_OUTER = 0xFF0D0D14;
    private static final int F_MID   = 0xFF1E1E28;
    private static final int F_INNER = 0xFF0A0A10;
    // Bar
    private static final int BAR_BG  = 0xFF12121A;
    // Target zone colours
    private static final int ZONE_GOOD    = 0x3044BBFF;
    private static final int ZONE_PERFECT = 0x50FFD700;
    private static final int ZONE_BORDER  = 0x6044BBFF;
    // Cursor
    private static final int CUR_CORE  = 0xFFFFFFFF;
    private static final int CUR_GLOW  = 0x50FFFFFF;
    private static final int CUR_TRAIL = 0x18FFFFFF;
    // Strip
    private static final int S_PERFECT = 0xFFCC9900;
    private static final int S_GOOD    = 0xFF2E7D32;
    private static final int S_MISS    = 0xFF7B2222;
    private static final int S_ACTIVE  = 0xFF2A5588;
    private static final int S_EMPTY   = 0xFF1A1A22;
    private static final int S_BORDER  = 0xFF333340;
    // Results (complete screen)
    private static final int R_PERFECT    = 0xFFBB8800;
    private static final int R_PERFECT_HI = 0xFFDDAA22;
    private static final int R_GOOD       = 0xFF338833;
    private static final int R_GOOD_HI    = 0xFF44CC44;
    private static final int R_MISS       = 0xFF883333;
    private static final int R_MISS_HI    = 0xFFBB4444;
    private static final int DONE_BAR     = 0xFF44BB44;
    private static final int DONE_HI      = 0xFF66DD66;
    // Idle
    private static final int IDLE_TICK = 0xFF1E1E28;

    private float prevBarPos = 0.5f;

    public WidgetForgingMinigameV2(TileEntityForgingAnvil anvil) {
        this.anvil = anvil;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int w = getArea().width;
        int h = getArea().height;

        GuiDraw.drawRect(0, 0, w, h, F_OUTER);
        GuiDraw.drawRect(1, 1, w - 2, h - 2, F_MID);
        GuiDraw.drawRect(2, 2, w - 4, h - 4, F_INNER);

        if (!anvil.isForging()) {
            drawIdle(w);
            return;
        }

        int pass = anvil.getCurrentPass();
        if (pass >= PASSES) {
            drawComplete(w);
            return;
        }

        drawActiveBar(w);
        drawPassStrip(w, pass);
        drawFeedback(w);
    }

    private void drawIdle(int w) {
        int iw = w - PAD * 2;
        GuiDraw.drawRect(PAD, PAD, iw, BAR_H, BAR_BG);
        for (int i = 0; i < iw; i += 8) {
            GuiDraw.drawRect(PAD + i, PAD + BAR_H / 2 - 1, 1, 2, IDLE_TICK);
        }
        int stripY = PAD + BAR_H + GAP;
        GuiDraw.drawRect(PAD, stripY, iw, STRIP_H, 0xFF16161E);
    }

    private void drawComplete(int w) {
        int iw = w - PAD * 2;
        int segW = Math.max(2, iw / PASSES);
        for (int i = 0; i < PASSES; i++) {
            int sx = PAD + i * segW;
            int sw = Math.max(1, segW - 1);
            int score = anvil.getPassScore(i);
            int col, hi;
            if (score == 2)      { col = R_PERFECT; hi = R_PERFECT_HI; }
            else if (score == 1) { col = R_GOOD;    hi = R_GOOD_HI; }
            else                 { col = R_MISS;    hi = R_MISS_HI; }
            GuiDraw.drawRect(sx, PAD, sw, BAR_H, col);
            GuiDraw.drawRect(sx, PAD, sw, 1, hi);
        }
        int stripY = PAD + BAR_H + GAP;
        GuiDraw.drawRect(PAD, stripY, iw, STRIP_H, DONE_BAR);
        GuiDraw.drawRect(PAD, stripY, iw, 1, DONE_HI);
    }

    private void drawActiveBar(int w) {
        int iw = w - PAD * 2;
        float barPos = anvil.getClientBarPos();
        float tc = anvil.getTargetCenter();
        float thw = anvil.getTargetHalfWidth();

        // Bar background
        GuiDraw.drawRect(PAD, PAD, iw, BAR_H, BAR_BG);

        // Target zone outer band (Good)
        int zoneL = PAD + Math.max(0, (int)((tc - thw) * iw));
        int zoneR = PAD + Math.min(iw, (int)((tc + thw) * iw));
        if (zoneR > zoneL) {
            GuiDraw.drawRect(zoneL, PAD, zoneR - zoneL, BAR_H, ZONE_GOOD);
            GuiDraw.drawRect(zoneL, PAD, 1, BAR_H, ZONE_BORDER);
            GuiDraw.drawRect(zoneR - 1, PAD, 1, BAR_H, ZONE_BORDER);
        }

        // Target zone inner band (Perfect)
        float innerHW = thw * 0.45f;
        int perfL = PAD + Math.max(0, (int)((tc - innerHW) * iw));
        int perfR = PAD + Math.min(iw, (int)((tc + innerHW) * iw));
        if (perfR > perfL) {
            GuiDraw.drawRect(perfL, PAD, perfR - perfL, BAR_H, ZONE_PERFECT);
        }

        // Centre marker
        int centX = PAD + (int)(tc * iw);
        if (centX > PAD && centX < PAD + iw) {
            GuiDraw.drawRect(centX, PAD, 1, BAR_H, 0x60FFD700);
        }

        // Cursor trail
        int curX = PAD + (int)(barPos * iw);
        int prevX = PAD + (int)(prevBarPos * iw);
        int trailL = Math.min(curX, prevX);
        int trailR = Math.max(curX, prevX);
        if (trailR - trailL > 2 && trailR - trailL < iw / 2) {
            GuiDraw.drawRect(trailL, PAD + BAR_H / 2 - 1, trailR - trailL, 2, CUR_TRAIL);
        }
        prevBarPos = barPos;

        // Cursor glow + core
        GuiDraw.drawRect(curX - 2, PAD, 6, BAR_H, CUR_GLOW);
        GuiDraw.drawRect(curX, PAD, 2, BAR_H, CUR_CORE);

        // Bar edge highlights
        GuiDraw.drawRect(PAD, PAD, iw, 1, 0x20FFFFFF);
        GuiDraw.drawRect(PAD, PAD + BAR_H - 1, iw, 1, 0xFF080810);
    }

    private void drawPassStrip(int w, int currentPass) {
        int iw = w - PAD * 2;
        int stripY = PAD + BAR_H + GAP;
        int segW = Math.max(2, iw / PASSES);
        for (int i = 0; i < PASSES; i++) {
            int sx = PAD + i * segW;
            int sw = Math.max(1, segW - 1);
            int col;
            if (i < currentPass) {
                int score = anvil.getPassScore(i);
                col = score == 2 ? S_PERFECT : score == 1 ? S_GOOD : S_MISS;
            } else if (i == currentPass) {
                col = S_ACTIVE;
            } else {
                col = S_EMPTY;
            }
            GuiDraw.drawRect(sx, stripY, sw, STRIP_H, col);
            if (i < currentPass) {
                int score = anvil.getPassScore(i);
                int hi = score == 2 ? 0xFFDDAA22 : score == 1 ? 0xFF44AA44 : 0xFFAA4444;
                GuiDraw.drawRect(sx, stripY, sw, 1, hi);
            }
        }
        GuiDraw.drawBorderOutsideXYWH(PAD, stripY, iw, STRIP_H, S_BORDER);
    }

    private void drawFeedback(int w) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.world == null) return;

        long worldTime = mc.world.getTotalWorldTime();
        long worldMod = worldTime & 0x7FFFFFFFL;
        long lastMod = anvil.getLastActionTick() & 0x7FFFFFFFL;
        long delta = Math.abs(worldMod - lastMod);

        if (delta < 10) {
            float fade = 1f - (delta / 10f);
            int alpha = (int)(fade * 180) & 0xFF;
            int score = anvil.getLastActionScore();
            int baseCol = score == 2 ? 0xFFD700 : score == 1 ? 0x44FF44 : 0xFF4444;
            int flashCol = (alpha << 24) | baseCol;

            int iw = w - PAD * 2;
            GuiDraw.drawRect(PAD, PAD, iw, BAR_H, flashCol);

            if (delta < 6) {
                String label = score == 2 ? "\u00a76\u00a7lPERFECT!" :
                               score == 1 ? "\u00a7a\u00a7lGOOD!" :
                                            "\u00a7c\u00a7lMISS!";
                int textW = mc.fontRenderer.getStringWidth(label);
                int textX = PAD + (iw - textW) / 2;
                int textY = PAD + (BAR_H - 8) / 2;
                mc.fontRenderer.drawStringWithShadow(label, textX, textY, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public int getDefaultHeight() {
        return BAR_H + STRIP_H + GAP + PAD * 2;
    }
}
