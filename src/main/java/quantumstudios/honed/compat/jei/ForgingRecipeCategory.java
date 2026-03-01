package quantumstudios.honed.compat.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import quantumstudios.honed.Tags;
import quantumstudios.honed.registry.HonedBlocks;

public class ForgingRecipeCategory implements IRecipeCategory<ForgingRecipeWrapper> {

    public static final String UID = Tags.MOD_ID + ".forging";
    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    public ForgingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(150, 40);
        icon = guiHelper.createDrawableIngredient(new ItemStack(HonedBlocks.FORGING_ANVIL));
        title = I18n.format("tile.honed.forging_anvil.name");
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
    public void setRecipe(IRecipeLayout recipeLayout, ForgingRecipeWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        stacks.init(0, true, 10, 11);
        stacks.init(1, false, 120, 11);
        stacks.set(ingredients);
    }
}
