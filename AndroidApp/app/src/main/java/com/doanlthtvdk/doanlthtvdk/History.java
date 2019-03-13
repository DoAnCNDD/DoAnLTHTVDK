package com.doanlthtvdk.doanlthtvdk;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Locale;

@IgnoreExtraProperties
public class History {
    public String id;
    public long time;
    public boolean verified;

    public History() {
    }

    public History(long time, boolean verified, String id) {
        this.time = time;
        this.verified = verified;
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("History{id='%s', time=%d, verified=%s}", id, time, verified, Locale.getDefault());
    }
}
