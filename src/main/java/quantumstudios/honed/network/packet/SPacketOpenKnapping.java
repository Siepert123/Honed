package quantumstudios.honed.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.oredict.OreDictionary;
import quantumstudios.honed.Honed;
import quantumstudios.honed.data.knapping.KnappingRecipeData;
import quantumstudios.honed.gui.KnappingGuiHandler;
import quantumstudios.honed.network.SPacketBase;
import quantumstudios.honed.registry.HonedRegistries;

public class SPacketOpenKnapping extends SPacketBase {

    @Override
    public void handle(PacketBuffer data, EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty()) return;
        if (!hasKnappingRecipe(held)) return;
        int quality = offhandQuality(player);
        player.openGui(Honed.instance(), KnappingGuiHandler.KNAPPING_GUI_ID,
                player.world, quality, (int) player.posY, (int) player.posZ);
    }

    private static int offhandQuality(EntityPlayer player) {
        ItemStack offhand = player.getHeldItemOffhand();
        if (offhand.isEmpty()) return 1;
        ResourceLocation rl = offhand.getItem().getRegistryName();
        if (rl == null) return 1;
        switch (rl.toString()) {
            case "minecraft:flint":   return 2;
            case "minecraft:diamond": return 3;
            case "minecraft:obsidian": return 4;
            default: return 1;
        }
    }

    private boolean hasKnappingRecipe(ItemStack stack) {
        ResourceLocation rl = stack.getItem().getRegistryName();
        if (rl == null) return false;
        String itemId = rl.toString();
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (KnappingRecipeData recipe : HonedRegistries.KNAPPING.values()) {
            if (recipe.inputItem == null) continue;
            if (recipe.inputItem.startsWith("ore:")) {
                String oreName = recipe.inputItem.substring(4);
                int targetOreId = OreDictionary.getOreID(oreName);
                for (int id : oreIds) {
                    if (id == targetOreId) return true;
                }
            } else if (recipe.inputItem.equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    public static FMLProxyPacket create() {
        PacketBuffer data = buf(SPacketOpenKnapping.class);
        return build(data);
    }
}
