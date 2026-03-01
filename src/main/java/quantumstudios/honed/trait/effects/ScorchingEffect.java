package quantumstudios.honed.trait.effects;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import quantumstudios.honed.api.ITraitEffect;
import quantumstudios.honed.api.TraitContext;

public class ScorchingEffect implements ITraitEffect {

    @Override
    public void onHit(TraitContext ctx, TraitContext.TraitInstance self,
                      ItemStack stack, EntityLivingBase target,
                      EntityLivingBase attacker, float damage) {
        int duration = (int) (self.getParam("duration", 3.0f)
                * self.level
                * ctx.getInteractionMultiplier(self.definition));
        if (duration > 0) {
            target.setFire(duration);
        }
    }
}
