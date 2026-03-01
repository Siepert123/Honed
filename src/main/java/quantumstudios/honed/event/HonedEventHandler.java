package quantumstudios.honed.event;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import quantumstudios.honed.Tags;
import quantumstudios.honed.config.HonedConfig;
import quantumstudios.honed.item.tool.ItemHonedTool;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.tool.ToolStats;
import quantumstudios.honed.trait.TraitEffectRegistry;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class HonedEventHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        EntityPlayer player = event.getPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemHonedTool)) return;
        int levelBefore = ToolNBT.getLevel(stack);
        IBlockState state = event.getState();
        BlockPos pos = event.getPos();
        TraitEffectRegistry.onMine(stack, event.getWorld(), state, pos, player);
        ToolNBT.addXp(stack, HonedConfig.xpPerMine);
        checkLevelUp(stack, player, levelBefore);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource() == null) return;
        if (!(event.getSource().getTrueSource() instanceof EntityLivingBase)) return;
        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();
        ItemStack stack = attacker.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemHonedTool)) return;
        int levelBefore = ToolNBT.getLevel(stack);
        TraitEffectRegistry.onHit(stack, event.getEntityLiving(), attacker, event.getAmount());
        ToolNBT.addXp(stack, HonedConfig.xpPerHit);
        checkLevelUp(stack, attacker, levelBefore);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        if (event.player.world.getTotalWorldTime() % 5 != 0) return;

        for (int i = 0; i < event.player.inventory.getSizeInventory(); i++) {
            ItemStack invStack = event.player.inventory.getStackInSlot(i);
            if (invStack.isEmpty()) continue;
            float temp = ToolNBT.getTemperature(invStack);
            if (temp > 0) {
                temp -= (float) HonedConfig.ambientCoolRate * 5;
                if (temp < 0) temp = 0;
                ToolNBT.setTemperature(invStack, temp);
            }
        }

        ItemStack stack = event.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemHonedTool)) return;
        TraitEffectRegistry.onTick(stack, event.player);
    }

    private static void checkLevelUp(ItemStack stack, EntityLivingBase entity, int levelBefore) {
        int levelAfter = ToolNBT.getLevel(stack);
        if (levelAfter <= levelBefore) return;
        ToolStats.recalculate(stack);
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.5f);
            player.sendMessage(new TextComponentTranslation("tooltip.honed.level_up", levelAfter));
        }
    }
}
