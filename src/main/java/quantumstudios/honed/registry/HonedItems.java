package quantumstudios.honed.registry;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import quantumstudios.honed.Tags;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.item.tool.ItemHonedAxe;
import quantumstudios.honed.item.tool.ItemHonedPickaxe;
import quantumstudios.honed.item.tool.ItemHonedShovel;
import quantumstudios.honed.item.tool.ItemHonedSword;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Objects;

public final class HonedItems {
    public static ItemBlock FORGING_ANVIL;

    public static Item PART;
    public static Item PICKAXE;
    public static Item SWORD;
    public static Item SHOVEL;
    public static Item AXE;

    public static void register(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        FORGING_ANVIL = new ItemBlock(HonedBlocks.FORGING_ANVIL);
        FORGING_ANVIL.setRegistryName("honed", "forging_anvil");
        FORGING_ANVIL.setTranslationKey("honed.forging_anvil");
        registry.register(FORGING_ANVIL);

        PART = new ItemHonedPart();
        PART.setRegistryName("honed", "part");
        PART.setTranslationKey("honed.part");
        registry.register(PART);
        PICKAXE = new ItemHonedPickaxe();
        PICKAXE.setRegistryName("honed", "pickaxe");
        PICKAXE.setTranslationKey("honed.pickaxe");
        registry.register(PICKAXE);
        SWORD = new ItemHonedSword();
        SWORD.setRegistryName("honed", "sword");
        SWORD.setTranslationKey("honed.sword");
        registry.register(SWORD);
        SHOVEL = new ItemHonedShovel();
        SHOVEL.setRegistryName("honed", "shovel");
        SHOVEL.setTranslationKey("honed.shovel");
        registry.register(SHOVEL);
        AXE = new ItemHonedAxe();
        AXE.setRegistryName("honed", "axe");
        AXE.setTranslationKey("honed.axe");
        registry.register(AXE);
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        model(FORGING_ANVIL);

        model(PART);
        model(PICKAXE);
        model(SWORD);
        model(SHOVEL);
        model(AXE);
    }

    @SideOnly(Side.CLIENT)
    private static void model(Item item, int metadata, ResourceLocation location) {
        Objects.requireNonNull(location, "Model location cannot be null");
        ModelLoader.setCustomModelResourceLocation(item, metadata,
                new ModelResourceLocation(location, "inventory")
        );
    }
    @SideOnly(Side.CLIENT)
    private static void model(Item item, int metadata, String location) {
        model(item, metadata, new ResourceLocation(Tags.MOD_ID, location));
    }
    @SideOnly(Side.CLIENT)
    private static void model(Item item, String location) {
        model(item, 0, location);
    }
    @SideOnly(Side.CLIENT)
    private static void model(Item item) {
        model(item, 0, item.getRegistryName());
    }
}
