package quantumstudios.honed.trait.effects;

import net.minecraft.item.ItemStack;
import quantumstudios.honed.api.ITraitEffect;
import quantumstudios.honed.api.TraitContext;

public class AttackBonusEffect implements ITraitEffect {
    @Override
    public float modifyStat(TraitContext ctx, TraitContext.TraitInstance self,
                            String stat, float currentValue) {
        if (!"attackDamage".equals(stat)) return currentValue;
        float bonus    = self.getParam("bonus", self.getParam("amount", 1.0f));
        float interMul = ctx.getInteractionMultiplier(self.definition);
        return currentValue + bonus * self.level * interMul;
    }
}
