package quantumstudios.honed.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import quantumstudios.honed.data.forging.ForgingRecipe;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.tool.ToolNBT;

import java.util.Collections;

public class ForgingRecipeWrapper implements IRecipeWrapper {

    private final ForgingRecipe recipe;

    public ForgingRecipeWrapper(ForgingRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ItemStack input = getInputStack();
        ingredients.setInput(VanillaTypes.ITEM, input);

        int meta = ItemHonedPart.getMetaForPartType(recipe.outputPartType);
        ItemStack output = new ItemStack(HonedItems.PART, 1, meta);
        ToolNBT.setPartData(output, recipe.outputPartType, recipe.materialId, 2);
        ingredients.setOutput(VanillaTypes.ITEM, output);
    }

    private ItemStack getInputStack() {
        if (recipe.inputItem == null) return ItemStack.EMPTY;
        if (recipe.inputItem.startsWith("ore:")) {
            String oreName = recipe.inputItem.substring(4);
            java.util.List<ItemStack> ores = net.minecraftforge.oredict.OreDictionary.getOres(oreName);
            return ores.isEmpty() ? ItemStack.EMPTY : ores.get(0).copy();
        }
        Item item = Item.REGISTRY.getObject(new ResourceLocation(recipe.inputItem));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }
}
