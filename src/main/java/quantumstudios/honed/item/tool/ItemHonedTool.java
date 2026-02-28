package quantumstudios.honed.item.tool;

import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.tool.ToolStats;
import quantumstudios.honed.trait.TraitEffectRegistry;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.block.state.IBlockState;
import com.google.common.collect.Multimap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.SharedMonsterAttributes;
import java.util.UUID;
import java.util.Collections;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.resources.I18n;
import java.util.List;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import quantumstudios.honed.data.part.PartSchema;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.data.material.MaterialDefinition;
import java.util.Map;
import java.util.Collection;
import quantumstudios.honed.tool.ToolNBT;

public abstract class ItemHonedTool extends ItemTool {
    private final String toolType; // "pickaxe", "sword", etc.
    private final String toolClass;

    public ItemHonedTool(String toolType) {
        super(1.0f, -2.8f, ToolMaterial.IRON, Collections.<Block>emptySet());
        this.toolType = toolType;
        this.toolClass = toolType;
        setMaxStackSize(1);
        setHasSubtypes(false);
        setMaxDamage(1); // will be overridden by NBT
        this.setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        return ToolNBT.getStat(stack, "miningSpeed");
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot equipmentSlot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(equipmentSlot, stack);
        if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", ToolNBT.getStat(stack, "attackDamage"), 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", ToolNBT.getStat(stack, "attackSpeed"), 0));
        }
        return multimap;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return (int) ToolNBT.getStat(stack, "durability");
    }

    @Override
    public Set<String> getToolClasses(ItemStack stack) {
        return Collections.singleton(toolClass);
    }

    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return (int) ToolNBT.getStat(stack, "harvestLevel");
    }

    @Override
    public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
        int lvl = (int) ToolNBT.getStat(stack, "harvestLevel");
        try {
            int req = state.getBlock().getHarvestLevel(state);
            return lvl >= req;
        } catch (Exception e) {
            return super.canHarvestBlock(state, stack);
        }
    }

    public String getToolType() { return toolType; }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!this.isInCreativeTab(tab)) return;
        // spawn one example of each material as a complete tool
        PartSchema schema = HonedRegistries.getPartSchema(toolType);
        if (schema == null || schema.partSlots == null) return;
        Collection<MaterialDefinition> mats = HonedRegistries.MATERIALS.values();
        for (MaterialDefinition mat : mats) {
            ItemStack stack = new ItemStack(this);
            // populate every slot with the same material
            for (String slot : schema.partSlots.keySet()) {
                ToolNBT.setMaterial(stack, slot, mat.id);
            }
            onToolUpdated(stack);
            items.add(stack);
        }
    }

    // Called when construction NBT changes — rebuilds stats and applies enchants from traits
    public static void onToolUpdated(ItemStack stack) {
        ToolStats.recalculate(stack);
        TraitEffectRegistry.onToolAssembled(stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        // choose language name from primary slot material
        String display = "";
        int color = 0xFFFFFF;
        PartSchema schema = HonedRegistries.getPartSchema(toolType);
        if (schema != null && schema.partSlots != null) {
            String primarySlot = null;
            for (Map.Entry<String, PartSchema.SlotDef> entry : schema.partSlots.entrySet()) {
                PartSchema.SlotDef def = entry.getValue();
                if (def.isPrimary || def.primary) {
                    primarySlot = entry.getKey();
                    break;
                }
            }
            if (primarySlot == null && !schema.partSlots.isEmpty()) {
                primarySlot = schema.partSlots.keySet().iterator().next();
            }
            if (primarySlot != null) {
                String matId = ToolNBT.getMaterial(stack, primarySlot);
                if (matId != null && !matId.isEmpty()) {
                    MaterialDefinition mat = HonedRegistries.getMaterial(matId);
                    if (mat != null) {
                        display = mat.displayName;
                        color = mat.color;
                    }
                }
            }
        }
        // apply hex color formatting (§xrrggbb)
        String colorCode = getColorCode(color);
        return colorCode + I18n.format(this.getTranslationKey(stack) + ".name", display);
    }

    private String getColorCode(int rgb) {
        // build §x§r§r§g§g§b§b
        String hex = String.format("%06x", rgb & 0xFFFFFF);
        StringBuilder sb = new StringBuilder("\u00A7x");
        for (char c : hex.toCharArray()) {
            sb.append("\u00A7").append(c);
        }
        return sb.toString();
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.durability", ToolStats.getStat(stack, "durability")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.miningSpeed", ToolStats.getStat(stack, "miningSpeed")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.harvestLevel", ToolStats.getStat(stack, "harvestLevel")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.attackDamage", ToolStats.getStat(stack, "attackDamage")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.attackSpeed", ToolStats.getStat(stack, "attackSpeed")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.enchantability", ToolStats.getStat(stack, "enchantability")));

        if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add(" ");
            tooltip.add(I18n.format("item.honed.tooltip.parts"));
            PartSchema schema = HonedRegistries.getPartSchema(toolType);
            if (schema != null && schema.partSlots != null) {
                for (String slot : schema.partSlots.keySet()) {
                    String matId = ToolNBT.getMaterial(stack, slot);
                    String matName = matId.isEmpty() ? "?" : I18n.format("material." + matId + ".name");
                    tooltip.add("  " + slot + ": " + matName);
                }
            }
        }
    }
}
