package quantumstudios.honed.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import quantumstudios.honed.data.forging.ForgingRecipe;
import quantumstudios.honed.data.knapping.KnappingRecipeData;
import quantumstudios.honed.registry.HonedBlocks;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.registry.HonedRegistries;

import java.util.ArrayList;
import java.util.List;

@JEIPlugin
public class HonedJEIPlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
        ISubtypeRegistry.ISubtypeInterpreter interp = stack -> {
            NBTTagCompound tag = stack.getTagCompound();
            return tag == null ? "" : tag.toString();
        };
        subtypeRegistry.registerSubtypeInterpreter(HonedItems.PICKAXE, interp);
        subtypeRegistry.registerSubtypeInterpreter(HonedItems.SWORD, interp);
        subtypeRegistry.registerSubtypeInterpreter(HonedItems.SHOVEL, interp);
        subtypeRegistry.registerSubtypeInterpreter(HonedItems.AXE, interp);
        subtypeRegistry.registerSubtypeInterpreter(HonedItems.HOE, interp);
        subtypeRegistry.registerSubtypeInterpreter(HonedItems.PART, interp);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new ForgingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCategories(new KnappingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registry) {
        List<ForgingRecipeWrapper> wrappers = new ArrayList<>();
        for (ForgingRecipe recipe : HonedRegistries.FORGING.values()) {
            wrappers.add(new ForgingRecipeWrapper(recipe));
        }
        registry.addRecipes(wrappers, ForgingRecipeCategory.UID);
        registry.addRecipeCatalyst(new ItemStack(HonedBlocks.FORGING_ANVIL),
                ForgingRecipeCategory.UID);

        List<KnappingRecipeWrapper> knappingWrappers = new ArrayList<>();
        for (KnappingRecipeData recipe : HonedRegistries.KNAPPING.values()) {
            knappingWrappers.add(new KnappingRecipeWrapper(recipe));
        }
        registry.addRecipes(knappingWrappers, KnappingRecipeCategory.UID);

        java.util.Set<String> catalystItems = new java.util.LinkedHashSet<>();
        for (KnappingRecipeData recipe : HonedRegistries.KNAPPING.values()) {
            if (recipe.inputItem != null) catalystItems.add(recipe.inputItem);
        }
        for (String inputItem : catalystItems) {
            if (inputItem.startsWith("ore:")) {
                String oreName = inputItem.substring(4);
                java.util.List<ItemStack> ores = net.minecraftforge.oredict.OreDictionary.getOres(oreName);
                for (ItemStack ore : ores) {
                    registry.addRecipeCatalyst(ore.copy(), KnappingRecipeCategory.UID);
                }
            } else {
                net.minecraft.item.Item item = net.minecraft.item.Item.REGISTRY.getObject(
                        new net.minecraft.util.ResourceLocation(inputItem));
                if (item != null) registry.addRecipeCatalyst(new ItemStack(item), KnappingRecipeCategory.UID);
            }
        }
    }
}
