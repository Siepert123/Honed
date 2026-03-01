package quantumstudios.honed.api;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import quantumstudios.honed.data.trait.TraitDefinition;

import java.util.*;

public class TraitContext {

    private final ItemStack stack;
    private final Map<ResourceLocation, TraitInstance> activeTraits = new LinkedHashMap<>();

    public TraitContext(ItemStack stack) {
        this.stack = stack;
    }

    public void addTrait(TraitDefinition def, int level) {
        ResourceLocation rl = new ResourceLocation(def.id);
        TraitInstance existing = activeTraits.get(rl);
        if (existing != null) {
            activeTraits.put(rl, new TraitInstance(def, existing.level + level));
        } else {
            activeTraits.put(rl, new TraitInstance(def, level));
        }
    }

    public boolean hasTrait(String id)             { return activeTraits.containsKey(new ResourceLocation(id)); }
    public boolean hasTrait(ResourceLocation id)    { return activeTraits.containsKey(id); }

    public int getTraitLevel(String id) {
        TraitInstance inst = activeTraits.get(new ResourceLocation(id));
        return inst != null ? inst.level : 0;
    }

    public TraitInstance get(String id) {
        return activeTraits.get(new ResourceLocation(id));
    }

    public Collection<TraitInstance> getAll() {
        return Collections.unmodifiableCollection(activeTraits.values());
    }

    public int size() { return activeTraits.size(); }

    public ItemStack getStack() { return stack; }

    public float getInteractionMultiplier(TraitDefinition def) {
        float mult = 1.0f;
        if (def.synergies != null) {
            for (String sid : def.synergies) {
                if (hasTrait(sid)) {
                    mult *= (def.synergyMultiplier > 0f) ? def.synergyMultiplier : 1.25f;
                }
            }
        }
        if (def.conflicts != null) {
            for (String cid : def.conflicts) {
                if (hasTrait(cid)) {
                    mult *= (def.conflictMultiplier > 0f) ? def.conflictMultiplier : 0.5f;
                }
            }
        }
        return mult;
    }

    public static class TraitInstance {
        public final TraitDefinition definition;
        public final int level;

        public TraitInstance(TraitDefinition definition, int level) {
            this.definition = definition;
            this.level = level;
        }

        public float getParam(String key, float defaultValue) {
            if (definition.params == null) return defaultValue;
            return definition.params.getOrDefault(key, defaultValue);
        }
    }
}
