package quantumstudios.honed.trait.effects;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import quantumstudios.honed.api.ITraitEffect;
import quantumstudios.honed.api.TraitContext;

import java.util.List;

public class MagneticPullEffect implements ITraitEffect {
    @Override
    public void onMine(TraitContext ctx, TraitContext.TraitInstance self,
                       ItemStack stack, World world, IBlockState state,
                       BlockPos pos, EntityLivingBase miner) {
        if (world.isRemote || !(miner instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) miner;
        double range = self.getParam("range", 5.0f)
                * self.level
                * ctx.getInteractionMultiplier(self.definition);
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class,
                player.getEntityBoundingBox().grow(range));
        for (EntityItem it : items) {
            it.motionX = (player.posX - it.posX) * 0.1;
            it.motionY = (player.posY - it.posY) * 0.1;
            it.motionZ = (player.posZ - it.posZ) * 0.1;
        }
    }
}
