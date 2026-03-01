package quantumstudios.honed.util;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import quantumstudios.honed.api.TraitContext;
import quantumstudios.honed.data.material.MaterialDefinition;
import quantumstudios.honed.data.part.PartSchema;
import quantumstudios.honed.data.trait.TraitDefinition;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.trait.TraitEffectRegistry;

import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class TooltipHelper {
    private TooltipHelper() {}

    private static final TextFormatting C_LABEL   = TextFormatting.GRAY;
    private static final TextFormatting C_VALUE   = TextFormatting.WHITE;
    private static final TextFormatting C_HEADER  = TextFormatting.GOLD;
    private static final TextFormatting C_TRAIT   = TextFormatting.YELLOW;
    private static final TextFormatting C_DESC    = TextFormatting.DARK_GRAY;
    private static final TextFormatting C_HINT    = TextFormatting.DARK_GRAY;
    private static final TextFormatting C_SYNERGY = TextFormatting.GREEN;
    private static final TextFormatting C_CONFLICT= TextFormatting.RED;

    public static void addToolTooltip(ItemStack stack, String toolType, List<String> tooltip) {
        int level = ToolNBT.getLevel(stack);
        int xpProgress = ToolNBT.getXpProgress(stack);
        int xpNeeded = ToolNBT.getXpToNextLevel(stack);
        tooltip.add(" " + TextFormatting.GREEN + I18n.format("tooltip.honed.level", level)
                + " " + C_LABEL + I18n.format("tooltip.honed.xp", xpProgress, xpNeeded));
        tooltip.add("");

        float durMult = ToolNBT.getStat(stack, "durabilityMult");
        if (durMult <= 0f) durMult = 1.0f;
        String durLabel = I18n.format("stat.honed.durabilityMult");
        tooltip.add(" " + C_LABEL + durLabel + ": " + C_VALUE + String.format("%.2fx", durMult));

        float attackDamage = ToolNBT.getStat(stack, "attackDamage");
        float attackSpeed  = ToolNBT.getStat(stack, "attackSpeed");
        if (attackDamage <= 0f || attackSpeed <= 0f) {
            float[] live = liveAttackStats(stack, toolType);
            if (attackDamage <= 0f) attackDamage = live[0];
            if (attackSpeed  <= 0f) attackSpeed  = live[1];
        }
        if (attackDamage > 0f) {
            addStatLine(tooltip, "attackDamage", 1.0f + attackDamage, "%.1f");
        }
        if (attackSpeed > 0f) {
            addStatLine(tooltip, "attackSpeed", attackSpeed, "%.2f");
        }

        addStatLine(tooltip, "miningSpeed",    ToolNBT.getStat(stack, "miningSpeed"),    "%.1f");
        addStatLine(tooltip, "harvestLevel",   ToolNBT.getStat(stack, "harvestLevel"),   "%.0f");
        addStatLine(tooltip, "enchantability", ToolNBT.getStat(stack, "enchantability"), "%.0f");

        if (GuiScreen.isShiftKeyDown()) {
            addTraitSection(stack, tooltip);
            addPartSection(stack, toolType, tooltip);
        } else {
            tooltip.add("");
            String raw = I18n.format("tooltip.honed.hold_shift");
            String formatted = raw.replace("SHIFT", TextFormatting.WHITE + "SHIFT" + C_HINT);
            tooltip.add(C_HINT + "" + TextFormatting.ITALIC + formatted);
        }
    }

    public static void addHeatTooltip(ItemStack stack, List<String> tooltip) {
        float temp = ToolNBT.getTemperature(stack);
        if (temp <= 0) return;

        float maxTemp = (float) quantumstudios.honed.config.HonedConfig.smelteryMaxTemp;
        float ratio = Math.min(temp / maxTemp, 1.0f);

        // Color the item name to match the heat glow
        if (!tooltip.isEmpty() && ratio > 0.04f) {
            String rawName = tooltip.get(0);
            String stripped = TextFormatting.getTextWithoutFormattingCodes(rawName);
            if (stripped != null && !stripped.isEmpty()) {
                tooltip.set(0, heatNameColor(ratio) + stripped);
            }
        }

        int segments = 20;
        int filled = Math.max(1, (int) (ratio * segments));

        StringBuilder bar = new StringBuilder(" ");
        for (int i = 0; i < segments; i++) {
            float segRatio = (float) i / (segments - 1);
            if (i < filled) {
                bar.append(heatSegColor(segRatio)).append('\u2588');
            } else {
                bar.append(TextFormatting.DARK_GRAY).append('\u2591');
            }
        }

        // Display temperature in Celsius (K - 273)
        int tempC = Math.max(0, Math.round(temp) - 273);
        tooltip.add(bar.toString());
        tooltip.add(" " + C_LABEL + tempC + " \u00b0C  " + heatLabel(ratio));

        // Look up the forging material for this item and show workability status
        net.minecraft.util.ResourceLocation rl = stack.getItem().getRegistryName();
        String itemId = rl != null ? rl.toString() : "";
        int[] oreIds = net.minecraftforge.oredict.OreDictionary.getOreIDs(stack);
        MaterialDefinition forgeMat = null;
        outer:
        for (quantumstudios.honed.data.forging.ForgingRecipe recipe : HonedRegistries.FORGING.values()) {
            if (recipe.inputItem == null) continue;
            boolean matches = false;
            if (recipe.inputItem.startsWith("ore:")) {
                int targetId = net.minecraftforge.oredict.OreDictionary.getOreID(recipe.inputItem.substring(4));
                for (int id : oreIds) if (id == targetId) { matches = true; break; }
            } else {
                matches = recipe.inputItem.equals(itemId);
            }
            if (matches) {
                MaterialDefinition m = HonedRegistries.getMaterial(recipe.materialId);
                if (m != null && "FORGE".equals(m.processingType)) { forgeMat = m; break outer; }
            }
        }
        if (forgeMat != null) {
            int minC = forgeMat.workingTempMin - 273;
            int maxC = forgeMat.workingTempMax - 273;
            if (temp >= forgeMat.workingTempMin && temp <= forgeMat.workingTempMax) {
                tooltip.add(" " + TextFormatting.GREEN + "\u2714 Forgeable  ("
                        + minC + "\u2013" + maxC + " \u00b0C)");
            } else if (temp > forgeMat.workingTempMax) {
                tooltip.add(" " + TextFormatting.YELLOW + "\u26a0 Too hot \u2014 cool below "
                        + maxC + " \u00b0C");
            } else {
                int need = forgeMat.workingTempMin - (int) temp; // difference same in K or \u00b0C
                tooltip.add(" " + TextFormatting.RED + "\u2718 Too cold \u2014 need +" + need + " \u00b0C more");
            }
        }
    }
    private static String heatNameColor(float ratio) {
        if (ratio < 0.12f) return TextFormatting.DARK_RED.toString();
        if (ratio < 0.35f) return TextFormatting.RED.toString();
        if (ratio < 0.60f) return TextFormatting.GOLD.toString();
        if (ratio < 0.82f) return TextFormatting.YELLOW.toString();
        return TextFormatting.WHITE.toString();
    }

    private static String heatSegColor(float p) {
        if (p < 0.25f) return TextFormatting.DARK_RED.toString();
        if (p < 0.45f) return TextFormatting.RED.toString();
        if (p < 0.65f) return TextFormatting.GOLD.toString();
        if (p < 0.85f) return TextFormatting.YELLOW.toString();
        return TextFormatting.WHITE.toString();
    }

    private static String heatLabel(float ratio) {
        if (ratio < 0.08f) return TextFormatting.GRAY       + "Lukewarm";
        if (ratio < 0.20f) return TextFormatting.DARK_RED   + "Warm";
        if (ratio < 0.35f) return TextFormatting.RED         + "Hot";
        if (ratio < 0.50f) return TextFormatting.RED         + "Red Hot";
        if (ratio < 0.65f) return TextFormatting.GOLD        + "Orange Hot";
        if (ratio < 0.78f) return TextFormatting.YELLOW      + "Yellow Hot";
        if (ratio < 0.92f) return TextFormatting.WHITE       + "White Hot";
        return TextFormatting.WHITE + "" + TextFormatting.BOLD + "Blazing";
    }

    public static void addPartTooltip(ItemStack stack, List<String> tooltip) {
        String matId = ToolNBT.getPartMaterial(stack);
        if (matId == null || matId.isEmpty()) return;
        MaterialDefinition mat = HonedRegistries.getMaterial(matId);
        if (mat == null) return;

        tooltip.add(ColorHelper.nearestFormatting(mat.color) + mat.displayName);

        // Show temperature status if heated (addHeatTooltip handles workability too)
        float temp = ToolNBT.getTemperature(stack);
        if (temp > 0) {
            tooltip.add("");
            addHeatTooltip(stack, tooltip);
            tooltip.add("");
        }

        if (mat.stats != null) {
            for (Map.Entry<String, Float> e : mat.stats.entrySet()) {
                String key = e.getKey();

                if ("handleMultiplier".equals(key)) continue;
                tooltip.add("  " + C_LABEL + capitalise(key) + ": " + C_VALUE + String.format("%.1f", e.getValue()));
            }
        }
        if (mat.traitIds != null && !mat.traitIds.isEmpty()) {
            tooltip.add("");
            tooltip.add(" " + C_HEADER + I18n.format("tooltip.honed.traits"));
            for (String tid : mat.traitIds) {
                TraitDefinition td = HonedRegistries.getTrait(tid);
                String name = (td != null && td.displayName != null) ? td.displayName : tid;
                tooltip.add("  " + C_TRAIT + name);
            }
        }
    }

    private static float[] liveAttackStats(ItemStack stack, String toolType) {
        PartSchema schema = HonedRegistries.getPartSchema(toolType);
        if (schema == null || schema.partSlots == null) return new float[]{0f, 0f};
        float dmgSum = 0f, spdSum = 0f, wSum = 0f;
        for (Map.Entry<String, PartSchema.SlotDef> e : schema.partSlots.entrySet()) {
            String matId = ToolNBT.getMaterial(stack, e.getKey());
            if (matId == null || matId.isEmpty()) continue;
            MaterialDefinition mat = HonedRegistries.getMaterial(matId);
            if (mat == null || mat.stats == null) continue;
            float w = (float) e.getValue().statWeight;
            dmgSum += mat.stats.getOrDefault("attackDamage", 0f) * w;
            spdSum += mat.stats.getOrDefault("attackSpeed",  0f) * w;
            wSum += w;
        }
        if (wSum <= 0f) return new float[]{0f, 0f};
        return new float[]{dmgSum / wSum, spdSum / wSum};
    }

    private static void addStatLine(List<String> tip, String key, float val, String fmt) {
        String label = I18n.format("stat.honed." + key);
        tip.add(" " + C_LABEL + label + ": " + C_VALUE + String.format(fmt, val));
    }

    private static void addTraitSection(ItemStack stack, List<String> tooltip) {
        tooltip.add("");
        tooltip.add(" " + C_HEADER + I18n.format("tooltip.honed.traits"));

        TraitContext ctx = TraitEffectRegistry.buildContext(stack);
        if (ctx.size() == 0) {
            tooltip.add("  " + C_DESC + I18n.format("tooltip.honed.no_traits"));
            return;
        }
        for (TraitContext.TraitInstance inst : ctx.getAll()) {
            String name = (inst.definition.displayName != null) ? inst.definition.displayName : inst.definition.id;
            String lvl  = (inst.level > 1) ? " " + roman(inst.level) : "";

            float im = ctx.getInteractionMultiplier(inst.definition);
            String tag = "";
            if (im > 1.01f)      tag = " " + C_SYNERGY  + "\u25B2";
            else if (im < 0.99f) tag = " " + C_CONFLICT + "\u25BC";

            StringBuilder sb = new StringBuilder();
            sb.append("  ").append(C_TRAIT).append(name).append(lvl).append(tag);
            if (inst.definition.description != null) {
                sb.append(" ").append(C_DESC).append(TextFormatting.ITALIC).append(inst.definition.description);
            }
            tooltip.add(sb.toString());
        }
    }

    private static void addPartSection(ItemStack stack, String toolType, List<String> tooltip) {
        tooltip.add("");
        tooltip.add(" " + C_HEADER + I18n.format("tooltip.honed.parts"));

        PartSchema schema = HonedRegistries.getPartSchema(toolType);
        if (schema == null || schema.partSlots == null) return;

        for (Map.Entry<String, PartSchema.SlotDef> entry : schema.partSlots.entrySet()) {
            String slot  = entry.getKey();
            String label = capitalise(entry.getValue().getPartType(slot));
            String matId = ToolNBT.getMaterial(stack, slot);

            if (matId == null || matId.isEmpty()) {
                tooltip.add("  " + C_LABEL + label + ": " + C_DESC + "—");
                continue;
            }
            MaterialDefinition mat = HonedRegistries.getMaterial(matId);
            if (mat == null) {
                tooltip.add("  " + C_LABEL + label + ": " + C_DESC + "?");
                continue;
            }
            TextFormatting mc = ColorHelper.nearestFormatting(mat.color);
            int quality = ToolNBT.getQuality(stack, slot);
            String stars = qualityStars(quality);
            tooltip.add("  " + C_LABEL + label + ": " + mc + mat.displayName
                    + (stars.isEmpty() ? "" : " " + stars));
        }
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
        if (q <= 0) return "";
        TextFormatting c;
        switch (q) {
            case 1:  c = TextFormatting.GRAY;   break;
            case 2:  c = TextFormatting.WHITE;   break;
            case 3:  c = TextFormatting.GREEN;   break;
            case 4:  c = TextFormatting.AQUA;    break;
            default: c = TextFormatting.GOLD;    break;
        }
        StringBuilder sb = new StringBuilder().append(c);
        int count = Math.min(q, 5);
        for (int i = 0; i < count; i++) sb.append('\u2605');
        return sb.toString();
    }

    private static String roman(int n) {
        switch (n) {
            case 1: return "I";   case 2: return "II";  case 3: return "III";
            case 4: return "IV";  case 5: return "V";
            default: return String.valueOf(n);
        }
    }
}
