package quantumstudios.honed.item.tool;

import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.Set;

public class ItemHonedHoe extends ItemHonedTool {

    public ItemHonedHoe() {
        super("hoe");
    }

    @Override
    public Set<String> getToolClasses(ItemStack stack) {
        return Collections.singleton("hoe");
    }
}
