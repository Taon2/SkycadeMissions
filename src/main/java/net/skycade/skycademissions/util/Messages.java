package net.skycade.skycademissions.util;

import net.skycade.SkycadeCore.Localization;
import net.skycade.SkycadeCore.Localization.Message;

public class Messages {

    //temp thing, these can all be the same/similar
    public static final Message COMPLETEMISSION = new Message("complete-mission", "&6You have completed %mission%!");
    public static final Message ALREADYCOMPLETED = new Message("already-completed", "&cYou have already completed %mission%!");
    public static final Message ALLTHREECOMPLETED = new Message("all-three-completed", "&6You have completed all three missions today! You have received an additional reward!");

    public static void init() {
        Localization.getInstance().registerMessages("skycade.missions",
                COMPLETEMISSION,
                ALREADYCOMPLETED,
                ALLTHREECOMPLETED
        );
    }
}
