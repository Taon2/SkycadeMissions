package net.skycade.skycademissions;

import net.skycade.skycademissions.missions.Mission;
import org.bukkit.entity.Player;

import java.util.*;

public class MissionsUser {

    private Player player;
    private Map<String, List<Count>> counts;
    private Map<String, Long> completed;

    private List<UUID> hitWithSnowball = new ArrayList<>();
    private List<String> kitKillKitsUsed = new ArrayList<>();

    public MissionsUser(Player player, Map<String, List<Count>> counts) {
        this.player = player;
        this.counts = counts;
    }

    public Player getPlayer() {
        return player;
    }

    public Map<String, List<Count>> getCounts() {
        return counts;
    }

    public Map<String, Long> getCompleted() {
        return completed;
    }

    public List<UUID> getHitWithSnowball() {
        return hitWithSnowball;
    }

    public List<String> getKitKillKitsUsed() {
        return kitKillKitsUsed;
    }

    public void addHitWithSnowball(UUID uuid) {
        hitWithSnowball.add(uuid);
    }

    public void addKitKillKitUsed(String kitType) {
        kitKillKitsUsed.add(kitType);
    }

    public void addCounter(Mission mission, String counted, int count) {
        boolean doesCountExist = false;

        if (mission.getHandle() == null || counts.get(mission.getHandle()) == null) return;

        for (Count missionCount : counts.get(mission.getHandle())) {
            if (!missionCount.getCounted().equals(counted)) continue;
            doesCountExist = true;
        }

        //Checks to see if there is an active counter within the last 24 hours
        if (!doesCountExist && !hasPlayerCompleted(mission)) {
            //Starts a new counter if there is not an active counter and the mission hasn't been completed
            counts.get(mission.getHandle()).add(new Count(counted, count));
        } else if (!hasPlayerCompleted(mission)){
            //Increments the existing counter as long as the mission hasn't been completed
            for (Count missionCount : counts.get(mission.getHandle())) {
                if (!missionCount.getCounted().equals(counted)) continue;
                missionCount.setCount(count);
            }
        }
    }

    public int getCurrentCount(Mission mission, String counted) {
        List<Map<?, ?>> section = mission.getParams();
        int currentCount = 0;

        for (Map<?, ?> s : section) {
            Object type = s.getOrDefault("type", null);
            if (type == null) continue;
            String countedThing = type.toString();

            if (!countedThing.equals(counted)) continue;

            boolean doesCountExist = false;
            if (counts.containsKey(mission.getHandle())) {
                for (Count missionCount : counts.get(mission.getHandle())) {
                    if (!missionCount.getCounted().equals(counted)) continue;
                    doesCountExist = true;
                }
            }

            //Checks to see if there is an active counter within the last 24 hours
            if (hasPlayerCompleted(mission)) {
                //Returns max value if already completed
                int amount = 1;
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;

                return amount;
            } else if (!doesCountExist && !hasPlayerCompleted(mission)) {
                //Starts a new counter if there is not an active counter and the mission hasn't been completed
                counts.computeIfAbsent(mission.getHandle(), k -> new ArrayList<>());
                counts.get(mission.getHandle()).add(new Count(counted, currentCount));
            } else {
                //Returns the existing counter
                for (Count missionCount : counts.get(mission.getHandle())) {
                    if (!missionCount.getCounted().equals(counted)) continue;
                    currentCount = missionCount.getCount();
                }
            }
        }

        return currentCount;
    }

    public boolean hasPlayerCompleted(Mission mission) {
        if (!completed.containsKey(mission.getHandle())) return false;

        long timestamp = completed.get(mission.getHandle());

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long timeInMillis = calendar.getTimeInMillis();
        return timestamp > timeInMillis;
    }

    public void completeMission(Mission mission) {
        completed.put(mission.getHandle(), System.currentTimeMillis());
    }

    public void setCompleted(Map<String, Long> completed) {
        this.completed = completed;
    }
}

