package quantumstudios.honed.trait.effects;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import quantumstudios.honed.api.ITraitEffect;
import quantumstudios.honed.api.TraitContext;

public class LifestealEffect implements ITraitEffect {

    @Override
    public void onHit(TraitContext ctx, TraitContext.TraitInstance self,
                      ItemStack stack, EntityLivingBase target,
                      EntityLivingBase attacker, float damage) {
        float percent = self.getParam("percent", 0.15f)
                * self.level
                * ctx.getInteractionMultiplier(self.definition);
        float heal = damage * percent;
        if (heal > 0) {
            attacker.heal(heal);
        }
    }
}
