package quantumstudios.honed.data.material;

import java.util.List;
import java.util.Map;

import java.util.List;
import java.util.Map;

public class MaterialDefinition {
    public String id;
    public String displayName;
    public int tier;
    public String processingType;   // "FORGE", "KNAP", "CARVE"
    public String forgingProfileId; // references a ForgingRecipe type
    public Map<String, Float> stats; // "speed"->6.0, "durability"->250, etc.
    // stat keys: speed, durability, harvestLevel, attackDamage, attackSpeed, enchantability, handleMultiplier
    public Map<String, StatModifier> statModifiers; // optional, for MUL/ADD ops
    public int color = 0xFFFFFF; // RGB hex; used for display and tool tinting
    public List<String> traitIds;
    public int workingTempMin;
    public int workingTempMax;
    public int meltingTemp;
    public String componentType;
    public List<String> componentTypes;

    public static class StatModifier {
        public String operation; // "MUL", "ADD", etc.
        public float value;
    }

    public static final MaterialDefinition EMPTY = createEmpty();

    private static MaterialDefinition createEmpty() {
        MaterialDefinition empty = new MaterialDefinition();
        empty.id = "empty";
        empty.displayName = "Empty";
        empty.tier = 0;
        empty.processingType = "";
        empty.forgingProfileId = "";
        empty.stats = new java.util.HashMap<>();
        empty.statModifiers = new java.util.HashMap<>();
        empty.traitIds = new java.util.ArrayList<>();
        empty.color = 0xFFFFFF;
        empty.workingTempMin = 0;
        empty.workingTempMax = 0;
        empty.meltingTemp = 0;
        return empty;
    }
}
