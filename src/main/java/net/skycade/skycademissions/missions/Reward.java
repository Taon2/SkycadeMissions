package net.skycade.skycademissions.missions;

import java.util.List;

public class Reward {

    private List<String> names;
    private List<String> commands;

    Reward (List<String> names, List<String> commands) {
        this.names = names;
        this.commands = commands;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getCommands() {
        return commands;
    }
}
