package quantumstudios.honed.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import quantumstudios.honed.data.knapping.KnappingRecipeData;
import quantumstudios.honed.data.material.MaterialDefinition;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.util.ColorHelper;

public class KnappingRecipeWrapper implements IRecipeWrapper {

    private final KnappingRecipeData recipe;
    private final int materialColor;

    public KnappingRecipeWrapper(KnappingRecipeData recipe) {
        this.recipe = recipe;
        MaterialDefinition mat = HonedRegistries.getMaterial(recipe.materialId);
        this.materialColor = mat != null ? ColorHelper.opaque(mat.color) : 0xFF8B4513;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, getInputStack());
        int meta = ItemHonedPart.getMetaForPartType(recipe.outputPartType);
        ItemStack output = new ItemStack(HonedItems.PART, 1, meta);
        ToolNBT.setPartData(output, recipe.outputPartType, recipe.materialId, 2);
        ingredients.setOutput(VanillaTypes.ITEM, output);
    }

    @Override
    public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        boolean[][] pattern = recipe.getParsedPattern();
        int gridX = 32;
        int gridY = 2;
        int cell = 14;
        int pitch = 15;
        int total = 5 * pitch;
        int carvedColor = 0xFF1A1A1A;

        Gui.drawRect(gridX - 1, gridY - 1, gridX + total + 1, gridY + total + 1, 0xFF373737);
        Gui.drawRect(gridX, gridY, gridX + total, gridY + total, 0xFF8B8B8B);

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                int cx = gridX + x * pitch;
                int cy = gridY + y * pitch;
                boolean carved = pattern[x][y];
                Gui.drawRect(cx, cy, cx + cell, cy + cell, carved ? carvedColor : materialColor);
            }
        }

        Gui.drawRect(110, 44, 132, 46, 0xFF555555);
        Gui.drawRect(126, 40, 128, 44, 0xFF555555);
        Gui.drawRect(128, 42, 130, 44, 0xFF555555);
        Gui.drawRect(126, 46, 128, 50, 0xFF555555);
        Gui.drawRect(128, 46, 130, 48, 0xFF555555);
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
