package quantumstudios.honed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import quantumstudios.honed.Tags;
import quantumstudios.honed.api.TraitContext;
import quantumstudios.honed.data.material.MaterialDefinition;
import quantumstudios.honed.data.part.PartSchema;
import quantumstudios.honed.data.trait.TraitDefinition;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.item.tool.ItemHonedTool;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.trait.TraitEffectRegistry;
import quantumstudios.honed.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class HonedTooltipRenderer {

    // Layout
    private static final int PAD     = 8;
    private static final int ICON    = 32;
    private static final int LINE    = 10;
    private static final int BAR     = 4;
    private static final int SWATCH  = 6;
    private static final int GAP     = 10;  // minimum px between stat label and value
    private static final int MIN_WIDTH = 160;
    private static final int MAX_WIDTH = 380;
    private static final float Z = 300f;

    // Colours
    private static final int BG         = 0xF0101018;
    private static final int SEP        = 0x28FFFFFF;
    private static final int COL_WHITE  = 0xFFFFFFFF;
    private static final int COL_GRAY   = 0xFFAAAAAA;
    private static final int COL_DARK   = 0xFF666666;
    private static final int COL_GREEN  = 0xFF55FF55;
    private static final int COL_GOLD   = 0xFFFFAA00;
    private static final int COL_YELLOW = 0xFFFFFF55;
    private static final int COL_RED    = 0xFFFF5555;
    private static final int XP_BG      = 0xFF1A1A2E;
    private static final int XP_FILL    = 0xFF4ADE80;

    private HonedTooltipRenderer() {}

    @SubscribeEvent
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getStack();
        if (stack.getItem() instanceof ItemHonedTool) {
            event.setCanceled(true);
            renderToolCard(stack, (ItemHonedTool) stack.getItem(),
                    event.getX(), event.getY(),
                    event.getScreenWidth(), event.getScreenHeight(),
                    event.getFontRenderer());
        } else if (stack.getItem() instanceof ItemHonedPart) {
            event.setCanceled(true);
            renderPartCard(stack,
                    event.getX(), event.getY(),
                    event.getScreenWidth(), event.getScreenHeight(),
                    event.getFontRenderer());
        }
    }

    private static void renderToolCard(ItemStack stack, ItemHonedTool toolItem,
                                       int mx, int my, int sw, int sh, FontRenderer font) {
        String toolType = toolItem.getToolType();
        boolean shift   = GuiScreen.isShiftKeyDown();

        // Raw stat values
        String name   = stack.getDisplayName();
        int    level  = ToolNBT.getLevel(stack);
        int    xpNow  = ToolNBT.getXpProgress(stack);
        int    xpMax  = ToolNBT.getXpToNextLevel(stack);
        int    maxDur = (int) ToolNBT.getStat(stack, "durability");
        int    curDur = maxDur > 0 ? Math.max(0, maxDur - stack.getItemDamage()) : 0;
        float  speed  = ToolNBT.getStat(stack, "miningSpeed");
        float  harvest= ToolNBT.getStat(stack, "harvestLevel");
        float  ench   = ToolNBT.getStat(stack, "enchantability");
        float  atkDmg = ToolNBT.getStat(stack, "attackDamage");
        float  atkSpd = ToolNBT.getStat(stack, "attackSpeed");
        int    accent = ColorHelper.opaque(getPrimaryColor(stack, toolType));

        String lvlText = I18n.format("tooltip.honed.level", level);
        String xpText  = xpNow + "/" + xpMax;

        // Stat rows: { label, value, statKey, rawValue }
        String[][]  statRows  = {
            { I18n.format("stat.honed.durability"),     curDur + "/" + maxDur,          "durability",     String.valueOf(maxDur) },
            { I18n.format("stat.honed.miningSpeed"),    String.format("%.1f", speed),   "miningSpeed",    String.valueOf(speed)  },
            { I18n.format("stat.honed.harvestLevel"),   String.format("%.0f", harvest), "harvestLevel",   String.valueOf(harvest)},
            { I18n.format("stat.honed.attackDamage"),   String.format("%.1f", atkDmg),  "attackDamage",   String.valueOf(atkDmg) },
            { I18n.format("stat.honed.attackSpeed"),    String.format("%.2f", atkSpd),  "attackSpeed",    String.valueOf(atkSpd) },
            { I18n.format("stat.honed.enchantability"), String.format("%.0f", ench),    "enchantability", String.valueOf(ench)   },
        };

        PartSchema schema       = HonedRegistries.getPartSchema(toolType);
        float      avgSlotW     = calcAvgSlotWeight(schema);
        int        partCount    = schema != null && schema.partSlots != null ? schema.partSlots.size() : 0;
        TraitContext ctx        = shift ? TraitEffectRegistry.buildContext(stack) : null;

        // ── width: measure every element that will actually appear ────────────
        int statColW = 0;
        for (String[] r : statRows)
            statColW = Math.max(statColW, font.getStringWidth(r[0]) + GAP + font.getStringWidth(r[1]));

        int headerW = font.getStringWidth(name) + ICON + PAD;

        int partsW = 0;
        if (shift && schema != null && schema.partSlots != null) {
            for (Map.Entry<String, PartSchema.SlotDef> e : schema.partSlots.entrySet()) {
                String slot    = e.getKey();
                String label   = capitalise(e.getValue().getPartType(slot));
                String matId   = ToolNBT.getMaterial(stack, slot);
                MaterialDefinition m = matId != null ? HonedRegistries.getMaterial(matId) : null;
                String matName = m != null ? m.displayName : "\u2014";
                int quality    = ToolNBT.getQuality(stack, slot);
                int rowW = SWATCH + 6
                         + font.getStringWidth(label) + GAP
                         + font.getStringWidth(matName) + GAP
                         + font.getStringWidth(qualityStars(quality)) + 2
                         + font.getStringWidth(qualityName(quality));
                partsW = Math.max(partsW, rowW);
            }
        }

        int traitsW = 0;
        if (ctx != null)
            for (TraitContext.TraitInstance inst : ctx.getAll())
                traitsW = Math.max(traitsW, font.getStringWidth("\u25B8 " + inst.definition.displayName));

        int w        = Math.min(Math.max(Math.max(Math.max(statColW, Math.max(headerW, Math.max(partsW, traitsW))), MIN_WIDTH) + PAD * 2, MIN_WIDTH), MAX_WIDTH);
        int contentW = w - PAD * 2;

        // ── height: count every rendered pixel row ───────────────────────────
        int h = PAD
              + ICON + 4      // icon + gap
              + BAR + 6       // xp bar + gap below
              + 4             // separator line
              + statRows.length * LINE
              + PAD;

        if (shift) {
            h += 4 + LINE + partCount * LINE; // sep + "Parts" + part rows
            if (ctx != null && ctx.size() > 0) {
                h += 4 + LINE;                // sep + "Traits"
                for (TraitContext.TraitInstance inst : ctx.getAll()) {
                    h += LINE;                // trait name
                    h += wrappedHeight(font, inst.definition.description, contentW - 12);
                }
                h += 2;                       // bottom breathing room
            }
        } else {
            h += 4 + LINE; // hint row
        }

        // ── screen position ──────────────────────────────────────────────────
        int x = mx + 12, y = my - 12;
        if (x + w > sw - 4) x = mx - w - 4;
        if (y + h > sh - 4) y = sh - h - 4;
        if (y < 4)           y = 4;

        // ── draw ─────────────────────────────────────────────────────────────
        beginTooltip();
        drawCardBg(x, y, w, h, accent);

        int cx = x + PAD, cy = y + PAD;

        // Header row
        renderItemIcon(stack, cx, cy);
        int tx = cx + ICON + 8;
        font.drawStringWithShadow(name,    tx, cy + 2,  COL_WHITE);
        font.drawStringWithShadow(lvlText, tx, cy + 14, COL_GREEN);
        font.drawStringWithShadow(xpText,  x + w - PAD - font.getStringWidth(xpText), cy + 14, COL_DARK);
        cy += ICON + 4;

        // XP bar
        drawSimpleBar(cx, cy, contentW, BAR, xpNow, xpMax, XP_BG, XP_FILL);
        cy += BAR + 6;

        // Stats
        drawSep(x, cy, w); cy += 4;
        for (String[] r : statRows) {
            float rawVal = Float.parseFloat(r[3]);
            cy = drawStatRow(font, cx, cy, contentW, r[0], r[1], statColor(r[2], rawVal, avgSlotW));
        }

        // Shift: parts + traits
        if (shift) {
            drawSep(x, cy, w); cy += 4;
            font.drawStringWithShadow(I18n.format("tooltip.honed.parts"), cx, cy, COL_GOLD);
            cy += LINE;

            if (schema != null && schema.partSlots != null) {
                for (Map.Entry<String, PartSchema.SlotDef> entry : schema.partSlots.entrySet()) {
                    String slot    = entry.getKey();
                    String label   = capitalise(entry.getValue().getPartType(slot));
                    String matId   = ToolNBT.getMaterial(stack, slot);
                    MaterialDefinition m = (matId != null && !matId.isEmpty()) ? HonedRegistries.getMaterial(matId) : null;
                    int    mc      = m != null ? ColorHelper.opaque(m.color) : 0xFF555555;
                    String matName = m != null ? m.displayName : "\u2014";
                    int    quality = ToolNBT.getQuality(stack, slot);
                    String qName   = qualityName(quality);
                    String qStars  = qualityStars(quality);
                    int    qCol    = qualityColor(quality);

                    drawSwatch(cx, cy + 2, SWATCH, mc);
                    font.drawStringWithShadow(label, cx + SWATCH + 4, cy, COL_GRAY);

                    int re = x + w - PAD;
                    font.drawStringWithShadow(qName,  re - font.getStringWidth(qName), cy, qCol);
                    re -= font.getStringWidth(qName) + 4;
                    font.drawStringWithShadow(qStars, re - font.getStringWidth(qStars), cy, qCol);
                    re -= font.getStringWidth(qStars) + 6;
                    font.drawStringWithShadow(matName, re - font.getStringWidth(matName), cy, mc);
                    cy += LINE;
                }
            }

            if (ctx != null && ctx.size() > 0) {
                drawSep(x, cy, w); cy += 4;
                font.drawStringWithShadow(I18n.format("tooltip.honed.traits"), cx, cy, COL_GOLD);
                cy += LINE;
                for (TraitContext.TraitInstance inst : ctx.getAll()) {
                    String tName = inst.definition.displayName != null ? inst.definition.displayName : inst.definition.id;
                    String lvl   = inst.level > 1 ? " " + roman(inst.level) : "";
                    font.drawStringWithShadow("\u25B8 " + tName + lvl, cx, cy, COL_YELLOW);
                    cy += LINE;
                    cy = drawWrapped(font, inst.definition.description, cx + 10, cy, contentW - 12, COL_DARK);
                }
            }
        } else {
            cy += 4;
            String hint = I18n.format("tooltip.honed.hold_shift")
                    .replace("SHIFT", "\u00a7f" + "SHIFT" + "\u00a78");
            font.drawStringWithShadow("\u00a78\u00a7o" + hint, cx, cy, COL_DARK);
        }

        endTooltip();
    }

    private static void renderPartCard(ItemStack stack, int mx, int my, int sw, int sh, FontRenderer font) {
        String matId = ToolNBT.getPartMaterial(stack);
        MaterialDefinition mat = (matId != null && !matId.isEmpty()) ? HonedRegistries.getMaterial(matId) : null;
        if (mat == null) return;

        int    accent = ColorHelper.opaque(mat.color);
        String name   = stack.getDisplayName();

        // Quality multiplier applied to displayed stats
        int    partQuality = ToolNBT.getPartQuality(stack);
        float  qualityMult = 0.50f + partQuality * 0.25f;

        // Stat rows (quality-adjusted values)
        List<String[]> statRows = new ArrayList<>();
        if (mat.stats != null) {
            for (Map.Entry<String, Float> e : mat.stats.entrySet()) {
                if ("handleMultiplier".equals(e.getKey())) continue;
                float adjusted = e.getValue() * qualityMult;
                statRows.add(new String[]{ capitalise(e.getKey()), String.format("%.1f", adjusted) });
            }
        }

        // Traits
        List<TraitDefinition> traits = new ArrayList<>();
        if (mat.traitIds != null) {
            for (String tid : mat.traitIds) {
                TraitDefinition td = HonedRegistries.getTrait(tid);
                if (td != null) traits.add(td);
            }
        }

        // Heat
        float   temp     = ToolNBT.getTemperature(stack);
        boolean heated   = temp > 0;
        boolean hasForge = "FORGE".equals(mat.processingType) && mat.workingTempMin > 0;
        int     maxTemp  = mat.meltingTemp > 0 ? mat.meltingTemp : 1800;

        // ── width ────────────────────────────────────────────────────────────
        int statColW = 0;
        for (String[] r : statRows)
            statColW = Math.max(statColW, font.getStringWidth(r[0]) + GAP + font.getStringWidth(r[1]));

        int traitW = 0;
        for (TraitDefinition td : traits)
            traitW = Math.max(traitW, font.getStringWidth("\u25B8 " + td.displayName));

        int nameW  = font.getStringWidth(name) + ICON + PAD;
        int innerW = Math.max(nameW, Math.max(statColW, traitW));
        int w      = Math.min(Math.max(innerW + PAD * 2, MIN_WIDTH), MAX_WIDTH);
        int contentW = w - PAD * 2;

        // ── height ───────────────────────────────────────────────────────────
        int h = PAD + ICON + 4;  // header
        h += statRows.size() * LINE;
        if (!traits.isEmpty()) {
            h += 6 + LINE;       // sep + "Traits"
            for (TraitDefinition td : traits) {
                h += LINE;       // trait name
                h += wrappedHeight(font, td.description, contentW - 12);
            }
        }
        if (heated) {
            h += 6 + LINE + BAR + 4;  // sep + temp row + bar + gap
            if (hasForge) h += LINE;  // status row
        }
        h += PAD;

        // ── position ─────────────────────────────────────────────────────────
        int x = mx + 12, y = my - 12;
        if (x + w > sw - 4) x = mx - w - 4;
        if (y + h > sh - 4) y = sh - h - 4;
        if (y < 4)           y = 4;

        // ── draw ─────────────────────────────────────────────────────────────
        beginTooltip();
        drawCardBg(x, y, w, h, accent);

        int cx = x + PAD, cy = y + PAD;

        renderItemIcon(stack, cx, cy);

        String pqName   = qualityName(partQuality);
        String pqStars  = qualityStars(partQuality);
        int    pqCol    = qualityColor(partQuality);
        int    tx       = cx + ICON + 8;

        font.drawStringWithShadow(name,          tx, cy + 3,  accent);
        font.drawStringWithShadow(mat.displayName, tx, cy + 13, COL_GRAY);

        // Quality right-aligned in header
        int re = x + w - PAD;
        font.drawStringWithShadow(pqName,      re - font.getStringWidth(pqName), cy + 13, pqCol);
        re -= font.getStringWidth(pqName) + 4;
        font.drawStringWithShadow(pqStars,     re - font.getStringWidth(pqStars), cy + 13, pqCol);

        cy += ICON + 4;

        // Stat rows
        for (String[] r : statRows)
            cy = drawStatRow(font, cx, cy, contentW, r[0], r[1], COL_WHITE);

        // Traits
        if (!traits.isEmpty()) {
            drawSep(x, cy, w); cy += 4;
            font.drawStringWithShadow(I18n.format("tooltip.honed.traits"), cx, cy, COL_GOLD);
            cy += LINE;
            for (TraitDefinition td : traits) {
                font.drawStringWithShadow("\u25B8 " + td.displayName, cx, cy, COL_YELLOW);
                cy += LINE;
                cy = drawWrapped(font, td.description, cx + 10, cy, contentW - 12, COL_DARK);
            }
        }

        // Heat section
        if (heated) {
            drawSep(x, cy, w); cy += 4;

            // Temperature label + value coloured to match the gradient
            String tempLabel = "Temperature";
            String tempVal   = (int) temp + " K";
            int    tempCol   = heatColor(temp, maxTemp);
            font.drawStringWithShadow(tempLabel, cx, cy, COL_GRAY);
            font.drawStringWithShadow(tempVal, x + w - PAD - font.getStringWidth(tempVal), cy, tempCol);
            cy += LINE + 1;

            // Gradient bar with workable-zone overlay and current-temp marker
            drawHeatBar(cx, cy, contentW, BAR, temp, maxTemp,
                    hasForge ? mat.workingTempMin : -1,
                    hasForge ? mat.workingTempMax : -1);
            cy += BAR + 4;

            // Status line
            if (hasForge) {
                if (temp >= mat.workingTempMin && temp <= mat.workingTempMax) {
                    font.drawStringWithShadow("\u00a7a\u2714 Forgeable", cx, cy, COL_WHITE);
                } else if (temp > mat.workingTempMax) {
                    font.drawStringWithShadow("\u00a7c\u26A0 Too hot \u2014 cool before forging", cx, cy, COL_WHITE);
                } else {
                    int needed = mat.workingTempMin - (int) temp;
                    font.drawStringWithShadow("\u00a7c\u2718 Too cold  (need +" + needed + " K)", cx, cy, COL_WHITE);
                }
                cy += LINE;
            }
        }

        endTooltip();
    }

    private static void beginTooltip() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, Z);
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
    }

    private static void endTooltip() {
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawCardBg(int x, int y, int w, int h, int accent) {
        Gui.drawRect(x, y, x + w, y + h, BG);
        Gui.drawRect(x, y, x + w, y + 2, accent);
        int border = (0x40 << 24) | (accent & 0x00FFFFFF);
        Gui.drawRect(x, y + 2, x + 1, y + h, border);
        Gui.drawRect(x + w - 1, y + 2, x + w, y + h, border);
        Gui.drawRect(x, y + h - 1, x + w, y + h, border);
    }

    private static void renderItemIcon(ItemStack stack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(2, 2, 1);
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }

    private static void drawSep(int x, int y, int w) {
        Gui.drawRect(x + 4, y, x + w - 4, y + 1, SEP);
    }

    private static void drawSimpleBar(int x, int y, int w, int h, int current, int max, int bg, int fill) {
        Gui.drawRect(x, y, x + w, y + h, bg);
        if (max > 0 && current > 0) {
            int fw = Math.min((int) ((float) current / max * w), w);
            Gui.drawRect(x, y, x + fw, y + h, fill);
        }
    }

    private static int drawStatRow(FontRenderer font, int x, int y, int w, String label, String value, int valueColor) {
        font.drawStringWithShadow(label, x, y, COL_GRAY);
        font.drawStringWithShadow(value, x + w - font.getStringWidth(value), y, valueColor);
        return y + LINE;
    }
    private static int drawWrapped(FontRenderer font, String text, int x, int y, int maxW, int color) {
        if (text == null || text.isEmpty()) return y;
        for (String line : font.listFormattedStringToWidth(text, maxW)) {
            font.drawStringWithShadow("\u00a7o" + line, x, y, color);
            y += LINE;
        }
        return y;
    }
    private static int wrappedHeight(FontRenderer font, String text, int maxW) {
        if (text == null || text.isEmpty()) return 0;
        return font.listFormattedStringToWidth(text, maxW).size() * LINE;
    }
    private static float calcAvgSlotWeight(PartSchema schema) {
        if (schema == null || schema.partSlots == null || schema.partSlots.isEmpty()) return 0.75f;
        float tw = 0;
        for (PartSchema.SlotDef d : schema.partSlots.values()) tw += d.statWeight;
        return tw / schema.partSlots.size();
    }
    private static void drawHeatBar(int x, int y, int w, int h, float temp, int maxTemp, int workMin, int workMax) {
        // Gradient pass: one pixel column at a time
        for (int i = 0; i < w; i++) {
            float t = (float) i / Math.max(w - 1, 1);
            int col = lerpHeat(t) | 0xFF000000;
            Gui.drawRect(x + i, y, x + i + 1, y + h, col);
        }
        // Workable zone overlay
        if (workMin > 0 && workMax > 0 && maxTemp > 0) {
            int zoneX1 = x + (int) ((float) workMin / maxTemp * w);
            int zoneX2 = x + (int) ((float) workMax / maxTemp * w);
            zoneX1 = Math.max(x, Math.min(zoneX1, x + w));
            zoneX2 = Math.max(x, Math.min(zoneX2, x + w));
            Gui.drawRect(zoneX1, y, zoneX2, y + h, 0x5000FF44);
            Gui.drawRect(zoneX1, y, zoneX1 + 1, y + h, 0xAA00FF44);
            Gui.drawRect(zoneX2 - 1, y, zoneX2, y + h, 0xAA00FF44);
        }
        // Current-temperature marker
        if (maxTemp > 0 && temp > 0) {
            int mx = x + (int) ((float) Math.min(temp, maxTemp) / maxTemp * (w - 1));
            Gui.drawRect(mx, y - 1, mx + 1, y + h + 1, 0xFF000000);
            Gui.drawRect(mx, y,     mx + 1, y + h,     0xFFFFFFFF);
        }
    }
    private static int lerpHeat(float t) {
        // stops: {t, r, g, b}
        float[][] stops = {
            {0.00f,   0,   0,   0},
            {0.15f, 150,   0,   0},
            {0.40f, 255,   0,   0},
            {0.65f, 255, 165,   0},
            {0.80f, 255, 255,   0},
            {1.00f, 255, 255, 255},
        };
        for (int i = 1; i < stops.length; i++) {
            if (t <= stops[i][0]) {
                float f = (t - stops[i-1][0]) / (stops[i][0] - stops[i-1][0]);
                int r = (int)(stops[i-1][1] + f * (stops[i][1] - stops[i-1][1]));
                int g = (int)(stops[i-1][2] + f * (stops[i][2] - stops[i-1][2]));
                int b = (int)(stops[i-1][3] + f * (stops[i][3] - stops[i-1][3]));
                return (r << 16) | (g << 8) | b;
            }
        }
        return 0xFFFFFF;
    }
    private static int heatColor(float temp, int maxTemp) {
        float t = maxTemp > 0 ? Math.min(temp / maxTemp, 1f) : 0f;
        return 0xFF000000 | lerpHeat(t);
    }

    private static void drawSwatch(int x, int y, int size, int color) {
        Gui.drawRect(x, y, x + size, y + size, color);
        Gui.drawRect(x, y, x + 1, y + size, 0x40FFFFFF);
        Gui.drawRect(x, y, x + size, y + 1, 0x40FFFFFF);
    }

    private static int getPrimaryColor(ItemStack stack, String toolType) {
        PartSchema schema = HonedRegistries.getPartSchema(toolType);
        if (schema == null || schema.partSlots == null) return 0xFFFFFF;
        String slot = null;
        for (Map.Entry<String, PartSchema.SlotDef> e : schema.partSlots.entrySet()) {
            if (e.getValue().getPrimary()) { slot = e.getKey(); break; }
        }
        if (slot == null && !schema.partSlots.isEmpty()) slot = schema.partSlots.keySet().iterator().next();
        if (slot != null) {
            String matId = ToolNBT.getMaterial(stack, slot);
            if (matId != null && !matId.isEmpty()) {
                MaterialDefinition mat = HonedRegistries.getMaterial(matId);
                if (mat != null) return mat.color;
            }
        }
        return 0xFFFFFF;
    }

    private static String capitalise(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.split("[_\\s]+")) {
            if (sb.length() > 0) sb.append(' ');
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static String qualityStars(int q) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(Math.max(q, 0), 5);
        for (int i = 0; i < count; i++) sb.append('\u2605');
        return sb.toString();
    }

    private static String qualityName(int q) {
        switch (q) {
            case 0: return "Ruined";
            case 1: return "Crude";
            case 2: return "Poor";
            case 3: return "Decent";
            case 4: return "Fine";
            case 5: return "Good";
            case 6: return "Excellent";
            case 7: return "Masterwork";
            case 8: return "Legendary";
            default: return q > 8 ? "Legendary" : "Ruined";
        }
    }

    private static int qualityColor(int q) {
        switch (q) {
            case 0: return 0xFF555555; // dark grey
            case 1: return 0xFF888888; // grey
            case 2: return 0xFFAAAAAA; // light grey
            case 3: return 0xFFFFFFFF; // white
            case 4: return 0xFF55FF55; // green
            case 5: return 0xFF00CC00; // dark green
            case 6: return 0xFF55FFFF; // aqua
            case 7: return 0xFFFFAA00; // gold
            case 8: return 0xFFFF55FF; // magenta
            default: return q > 8 ? 0xFFFF55FF : 0xFF555555;
        }
    }

    private static int statColor(String toolStatKey, float value, float slotWeightFactor) {
        float avg = materialAvg(toolStatKey) * slotWeightFactor;
        if (avg <= 0) return COL_WHITE;
        float ratio = value / avg;
        if (ratio > 1.1f) return COL_GREEN;
        if (ratio < 0.9f) return COL_RED;
        return COL_WHITE;
    }

    private static float materialAvg(String toolStatKey) {
        String matKey = "miningSpeed".equals(toolStatKey) ? "speed" : toolStatKey;
        float sum = 0;
        int n = 0;
        for (MaterialDefinition mat : HonedRegistries.MATERIALS.values()) {
            if (mat.stats == null) continue;
            Float v = mat.stats.get(matKey);
            if (v != null) { sum += v; n++; }
        }
        return n > 0 ? sum / n : 0;
    }

    private static String roman(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(n);
        }
    }
}
