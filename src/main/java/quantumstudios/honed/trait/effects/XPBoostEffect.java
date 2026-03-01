package quantumstudios.honed.trait.effects;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import quantumstudios.honed.api.ITraitEffect;
import quantumstudios.honed.api.TraitContext;
import quantumstudios.honed.tool.ToolNBT;

public class XPBoostEffect implements ITraitEffect {

    @Override
    public void onMine(TraitContext ctx, TraitContext.TraitInstance self,
                       ItemStack stack, World world, IBlockState state,
                       BlockPos pos, EntityLivingBase miner) {
        if (world.isRemote) return;
        int extra = Math.round(self.getParam("amount", 1.0f) * self.level
                * ctx.getInteractionMultiplier(self.definition));
        ToolNBT.addXp(stack, extra);
    }
}
