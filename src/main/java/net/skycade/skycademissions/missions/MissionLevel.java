package net.skycade.skycademissions.missions;

public enum MissionLevel {
    EASY(0),
    MEDIUM(1),
    HARD(2),
    ALLTHREE(3);

    private final int level;

    MissionLevel(int level) {
        this.level = level;
    }

    public boolean isHigherOrEqualThan(MissionLevel level) {
        return this.level >= level.level;
    }

    public boolean isLowerOrEqualThan(MissionLevel level) {
        return this.level <= level.level;
    }
}
