package net.skycade.skycademissions.util;

import net.skycade.SkycadeCore.Localization;

public class Messages {

    //temp thing, these can all be the same/similar
    public static final Localization.Message LOADING = new Localization.Message("loading", "&eLoading data for this chunk, please wait...");

    public static void init() {
        Localization.getInstance().registerMessages("skycade.missions",
                LOADING
        );
    }
}
