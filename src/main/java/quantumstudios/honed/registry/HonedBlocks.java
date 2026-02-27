package quantumstudios.honed.registry;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import quantumstudios.honed.Tags;
import quantumstudios.honed.block.BlockForgingAnvil;
import quantumstudios.honed.te.TileEntityForgingAnvil;
import quantumstudios.honed.tesr.ForgingAnvilRenderer;

public final class HonedBlocks {
    public static Block FORGING_ANVIL;

    public static void register(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        FORGING_ANVIL = new BlockForgingAnvil();
        FORGING_ANVIL.setRegistryName("honed", "forging_anvil");
        FORGING_ANVIL.setTranslationKey("honed.forging_anvil");
        registry.register(FORGING_ANVIL);
    }

    public static void registerTEs() {
        registerTE(TileEntityForgingAnvil.class, "forging_anvil");
    }

    private static <T extends TileEntity> void registerTE(Class<T> clazz, String name) {
        GameRegistry.registerTileEntity(clazz, new ResourceLocation(Tags.MOD_ID, name));
    }

    @SideOnly(Side.CLIENT)
    public static void registerTESRs() {
        registerTESR(TileEntityForgingAnvil.class, new ForgingAnvilRenderer());
    }

    private static <T extends TileEntity> void registerTESR(Class<T> clazz, TileEntitySpecialRenderer<T> tesr) {
        tesr.setRendererDispatcher(TileEntityRendererDispatcher.instance);
        TileEntityRendererDispatcher.instance.renderers.put(clazz, tesr);
    }
}
