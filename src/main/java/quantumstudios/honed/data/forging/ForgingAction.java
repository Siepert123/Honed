package quantumstudios.honed.data.forging;

public enum ForgingAction {
    HIT_LIGHT(3),
    HIT_MEDIUM(7),
    HIT_HEAVY(13),
    DRAW(-3),
    PUNCH(-5),
    UPSET(-13);

    public final int delta;

    ForgingAction(int delta) {
        this.delta = delta;
    }

    public static ForgingAction fromOrdinal(int ordinal) {
        ForgingAction[] values = values();
        if (ordinal >= 0 && ordinal < values.length) return values[ordinal];
        return HIT_LIGHT;
    }
}
