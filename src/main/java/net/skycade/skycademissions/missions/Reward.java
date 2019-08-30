package net.skycade.skycademissions.missions;

import java.util.List;

public class Reward {

    private MissionLevel level;
    private List<String> names;
    private List<String> commands;

    Reward (MissionLevel level, List<String> names, List<String> commands) {
        this.level = level;
        this.names = names;
        this.commands = commands;
    }

    public MissionLevel getLevel() {
        return level;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getCommands() {
        return commands;
    }
}
