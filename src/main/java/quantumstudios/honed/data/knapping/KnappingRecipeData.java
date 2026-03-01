package quantumstudios.honed.data.knapping;

import java.util.List;

public class KnappingRecipeData {
    public String id;
    public String inputItem;
    public String materialId;
    public String outputPartType;
    public String[] pattern;

    private transient boolean[][] parsed;

    public boolean[][] getParsedPattern() {
        if (parsed != null) return parsed;
        parsed = new boolean[5][5];
        if (pattern == null) return parsed;
        for (int y = 0; y < Math.min(pattern.length, 5); y++) {
            String row = pattern[y];
            for (int x = 0; x < Math.min(row.length(), 5); x++) {
                parsed[x][y] = row.charAt(x) == ' ';
            }
        }
        return parsed;
    }

    public boolean matches(boolean[][] grid) {
        boolean[][] expected = getParsedPattern();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                if (grid[x][y] != expected[x][y]) return false;
            }
        }
        return true;
    }
}
