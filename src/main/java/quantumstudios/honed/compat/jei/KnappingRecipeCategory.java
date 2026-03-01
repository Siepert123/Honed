package quantumstudios.honed.compat.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import quantumstudios.honed.Tags;

public class KnappingRecipeCategory implements IRecipeCategory<KnappingRecipeWrapper> {

    public static final String UID = Tags.MOD_ID + ".knapping";
    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    public KnappingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(160, 90);
        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.FLINT));
        title = "Knapping";
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getModName() {
        return Tags.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, KnappingRecipeWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        stacks.init(0, true, 0, 37);
        stacks.init(1, false, 138, 37);
        stacks.set(ingredients);
    }
}
