package quantumstudios.honed.data.forging;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;
import quantumstudios.honed.data.part.PartSchema;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.tool.ToolStats;
import quantumstudios.honed.trait.TraitEffectRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolAssemblyRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        List<ItemStack> parts = collectParts(inv);
        if (parts.isEmpty()) return false;
        return findMatch(parts) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        List<ItemStack> parts = collectParts(inv);
        MatchResult match = findMatch(parts);
        if (match == null) return ItemStack.EMPTY;

        Item toolItem = getToolItem(match.schema.toolType);
        if (toolItem == null) return ItemStack.EMPTY;

        ItemStack tool = new ItemStack(toolItem);
        for (Map.Entry<String, ItemStack> entry : match.slotToPart.entrySet()) {
            String slot = entry.getKey();
            ItemStack part = entry.getValue();
            String matId = ToolNBT.getPartMaterial(part);
            int quality = ToolNBT.getPartQuality(part);
            ToolNBT.setMaterial(tool, slot, matId);
            ToolNBT.setQuality(tool, slot, quality);
        }

        ToolStats.recalculate(tool);
        TraitEffectRegistry.onToolAssembled(tool);
        return tool;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    private static List<ItemStack> collectParts(InventoryCrafting inv) {
        List<ItemStack> parts = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (!(stack.getItem() instanceof ItemHonedPart)) return Collections.emptyList();
                parts.add(stack);
            }
        }
        return parts;
    }

    private static MatchResult findMatch(List<ItemStack> parts) {
        for (PartSchema schema : HonedRegistries.PART_SCHEMAS.values()) {
            if (schema.partSlots == null) continue;
            if (parts.size() != schema.partSlots.size()) continue;
            Map<String, ItemStack> slotToPart = tryMatch(schema, parts);
            if (slotToPart != null) return new MatchResult(schema, slotToPart);
        }
        return null;
    }

    private static Map<String, ItemStack> tryMatch(PartSchema schema, List<ItemStack> parts) {
        Map<String, ItemStack> result = new LinkedHashMap<>();
        List<ItemStack> remaining = new ArrayList<>(parts);

        for (Map.Entry<String, PartSchema.SlotDef> entry : schema.partSlots.entrySet()) {
            String slotName = entry.getKey();
            String requiredType = entry.getValue().getPartType(slotName);
            ItemStack matched = null;
            for (ItemStack part : remaining) {
                String partType = ItemHonedPart.getPartTypeForMeta(part.getMetadata());
                if (requiredType.equals(partType)) {
                    matched = part;
                    break;
                }
            }
            if (matched == null) return null;
            remaining.remove(matched);
            result.put(slotName, matched);
        }
        return remaining.isEmpty() ? result : null;
    }

    private static Item getToolItem(String toolType) {
        switch (toolType) {
            case "pickaxe": return HonedItems.PICKAXE;
            case "sword":   return HonedItems.SWORD;
            case "shovel":  return HonedItems.SHOVEL;
            case "axe":     return HonedItems.AXE;
            case "hoe":     return HonedItems.HOE;
            default:        return null;
        }
    }

    private static class MatchResult {
        final PartSchema schema;
        final Map<String, ItemStack> slotToPart;

        MatchResult(PartSchema schema, Map<String, ItemStack> slotToPart) {
            this.schema = schema;
            this.slotToPart = slotToPart;
        }
    }
}
