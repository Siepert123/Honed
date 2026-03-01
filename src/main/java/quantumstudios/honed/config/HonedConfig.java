package quantumstudios.honed.config;

import net.minecraftforge.common.config.Config;
import quantumstudios.honed.Tags;

@Config(modid = Tags.MOD_ID)
public class HonedConfig {

    @Config.Comment("Base XP gained per block mined with a Honed tool")
    @Config.RangeInt(min = 0, max = 100)
    public static int xpPerMine = 1;

    @Config.Comment("Base XP gained per mob hit with a Honed tool")
    @Config.RangeInt(min = 0, max = 100)
    public static int xpPerHit = 2;

    @Config.Comment("Stat bonus per tool level (0.02 = 2% per level)")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double levelBonusPerLevel = 0.02;

    @Config.Comment("Exponent for the XP-per-level curve: floor(100 * level^exponent)")
    @Config.RangeDouble(min = 1.0, max = 3.0)
    public static double levelCurveExponent = 1.5;

    @Config.Comment("Enable verbose debug logging")
    public static boolean debug = false;

    @Config.Comment("Maximum temperature the smeltery can reach")
    @Config.RangeDouble(min = 100.0, max = 10000.0)
    public static double smelteryMaxTemp = 2000.0;

    @Config.Comment("Smeltery temperature increase per tick while fuel is burning")
    @Config.RangeDouble(min = 0.01, max = 10.0)
    public static double smelteryHeatRate = 0.5;

    @Config.Comment("Smeltery temperature decrease per tick when no fuel is burning")
    @Config.RangeDouble(min = 0.01, max = 10.0)
    public static double smelteryCoolRate = 0.25;

    @Config.Comment("Temperature decrease per tick on items outside the smeltery (ambient cooling)")
    @Config.RangeDouble(min = 0.01, max = 10.0)
    public static double ambientCoolRate = 0.1;
}
