package net.skycade.skycademissions;

import net.skycade.skycademissions.missions.DailyMissionManager;

public class Count {

    private String counted;
    private int count;
    private long timestamp;

    Count(String counted, int count) {
        this.counted = counted;
        this.timestamp = DailyMissionManager.getInstance().getLastGenerated() + 86400000;
        this.count = count;
    }

    public String getCounted() {
        return counted;
    }

    int getCount() {
        return count;
    }

    void setCount(int count) {
        this.count = count;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
