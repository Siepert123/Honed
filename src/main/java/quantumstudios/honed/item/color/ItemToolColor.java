package quantumstudios.honed.item.color;

import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.data.material.MaterialDefinition;


public class ItemToolColor implements IItemColor {
    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        String slot;
        switch (tintIndex) {
            case 0: slot = "handle"; break;
            case 1: slot = "head"; break;
            case 2: slot = "binder"; break;
            default: return -1;
        }
        String matId = ToolNBT.getMaterial(stack, slot);
        if (matId == null || matId.isEmpty()) return -1;
        MaterialDefinition mat = HonedRegistries.getMaterial(matId);
        if (mat == null) return -1;
        return 0xFF000000 | (mat.color & 0xFFFFFF);
    }
}
