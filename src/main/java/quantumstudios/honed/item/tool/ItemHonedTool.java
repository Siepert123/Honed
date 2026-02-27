package quantumstudios.honed.item.tool;

import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.tool.ToolStats;
import quantumstudios.honed.trait.TraitEffectRegistry;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemStack;
import net.minecraft.block.state.IBlockState;
import com.google.common.collect.Multimap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.SharedMonsterAttributes;
import java.util.UUID;
import java.util.Collections;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.resources.I18n;
import java.util.List;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;

public abstract class ItemHonedTool extends ItemTool {
    private final String toolType; // "pickaxe", "sword", etc.
    private final String toolClass;

    public ItemHonedTool(String toolType) {
        super(1.0f, -2.8f, ToolMaterial.IRON, Collections.<Block>emptySet());
        // pass dummy values — real values come from NBT
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

    public String getToolType() { return toolType; }

    // Called when construction NBT changes — rebuilds stats and applies enchants from traits
    public static void onToolUpdated(ItemStack stack) {
        ToolStats.recalculate(stack);
        TraitEffectRegistry.onToolAssembled(stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.format(this.getTranslationKey(stack) + ".item", "materialtest");
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.durability", ToolStats.getStat(stack, "durability")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.miningSpeed", ToolStats.getStat(stack, "miningSpeed")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.harvestLevel", ToolStats.getStat(stack, "harvestLevel")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.attackDamage", ToolStats.getStat(stack, "attackDamage")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.attackSpeed", ToolStats.getStat(stack, "attackSpeed")));
        tooltip.add(I18n.format("item.honed." + toolType + ".tooltip.enchantability", ToolStats.getStat(stack, "enchantability")));
    }
}
