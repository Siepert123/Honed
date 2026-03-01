package quantumstudios.honed.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import quantumstudios.honed.Honed;
import quantumstudios.honed.Tags;
import quantumstudios.honed.client.HonedKeyBindings;
import quantumstudios.honed.data.knapping.KnappingRecipeData;
import quantumstudios.honed.network.packet.SPacketOpenKnapping;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.util.TooltipHelper;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public class HonedClientEventHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        // Skip Honed parts and tools — they add the heat tooltip themselves via addInformation
        if (stack.getItem() instanceof quantumstudios.honed.item.part.ItemHonedPart) return;
        if (stack.getItem() instanceof quantumstudios.honed.item.tool.ItemHonedTool) return;
        if (ToolNBT.getTemperature(stack) > 0) {
            TooltipHelper.addHeatTooltip(stack, event.getToolTip());
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        EntityPlayer player = mc.player;
        if (player == null) return;

        if (HonedKeyBindings.KNAP.isPressed()) {
            ItemStack held = player.getHeldItemMainhand();
            if (held.isEmpty()) return;
            if (hasKnappingRecipe(held)) {
                Honed.NETWORK.sendToServer(SPacketOpenKnapping.create());
            }
        }
    }

    private static boolean hasKnappingRecipe(ItemStack stack) {
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
}
