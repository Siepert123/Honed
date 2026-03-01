package quantumstudios.honed.trait.effects;

import quantumstudios.honed.api.ITraitEffect;
import quantumstudios.honed.api.TraitContext;

public class SpeedBonusEffect implements ITraitEffect {
    @Override
    public float modifyStat(TraitContext ctx, TraitContext.TraitInstance self,
                            String stat, float currentValue) {
        if (!"miningSpeed".equals(stat)) return currentValue;
        float bonus    = self.getParam("bonus", self.getParam("amount", 1.0f));
        float interMul = ctx.getInteractionMultiplier(self.definition);
        return currentValue + bonus * self.level * interMul;
    }
}
