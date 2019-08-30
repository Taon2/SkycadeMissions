package net.skycade.skycademissions.util;

import net.skycade.SkycadeCore.Localization;
import net.skycade.SkycadeCore.Localization.Message;

public class Messages {

    //temp thing, these can all be the same/similar
    public static final Message COMPLETEMISSION = new Message("complete-mission", "&6You completed %mission%!");
    public static final Message ALREADYCOMPLETED = new Message("already-completed", "&cYou have already completed %mission%!");
    public static final Message ALLTHREECOMPLETED = new Message("all-three-completed", "&6You completed all three missions today! You received &b&l%reward% &6as an additional reward!");
    public static final Message NEWDAILYMISSIONS = new Message("new-daily-missions", "&6&lThere are new missions to complete today! &c&l/missions");
    public static final Message REWARDWON = new Message("reward-won", "&6You received %reward% as your reward!");

    public static void init() {
        Localization.getInstance().registerMessages("skycade.missions",
                COMPLETEMISSION,
                ALREADYCOMPLETED,
                ALLTHREECOMPLETED,
                NEWDAILYMISSIONS,
                REWARDWON
        );
    }
}
