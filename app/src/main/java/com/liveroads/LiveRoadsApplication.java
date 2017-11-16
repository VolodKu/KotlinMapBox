package com.liveroads;

import android.app.Application;
import android.content.Context;

import com.liveroads.app.adviser.TimeToNextTurnNarrator;
import com.liveroads.app.adviser.VoiceAdviser;
import com.liveroads.app.adviser.tts.TTSReader;
import com.liveroads.common.log.LogMode;

public final class LiveRoadsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        com.liveroads.mapbox.InitKt.init(this);
        com.liveroads.common.log.LoggerFactoryKt.init(BuildConfig.DEBUG ? LogMode.DEBUG : LogMode.RELEASE);
        com.liveroads.common.devtools.DevToolsKt.init(this);

        TTSReader.INSTANCE.init(getApplicationContext());
        VoiceAdviser.INSTANCE.init(new TimeToNextTurnNarrator(getResources()), getResources());
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);
    }
}
