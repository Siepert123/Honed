package quantumstudios.honed.trait.effects;

import quantumstudios.honed.api.ITraitEffect;
import quantumstudios.honed.api.TraitContext;

public class DurabilityMultEffect implements ITraitEffect {
    @Override
    public float modifyStat(TraitContext ctx, TraitContext.TraitInstance self,
                            String stat, float currentValue) {
        if (!"durability".equals(stat)) return currentValue;
        float factor   = self.getParam("factor", 1.1f);
        float interMul = ctx.getInteractionMultiplier(self.definition);

        float effective = 1.0f + (factor - 1.0f) * interMul * self.level;
        return currentValue * effective;
    }
}
