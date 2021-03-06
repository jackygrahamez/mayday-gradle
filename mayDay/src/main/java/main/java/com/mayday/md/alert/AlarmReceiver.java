package com.mayday.md.alert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.mayday.md.common.ApplicationSettings;
import com.mayday.md.common.Intents;


public class AlarmReceiver extends BroadcastReceiver {

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(">>>>>>>>", "onReceive - AlarmReceiver");
        mContext = context.getApplicationContext();
        if(intent.getAction().equals(Intents.SEND_ALERT_ACTION)) {
            Log.e(">>>>>>>>", "alert update received in AlarmReceiver at current time in millis = " + System.currentTimeMillis() % 100000);
            getPanicMessage(mContext).send(getCurrentBestLocation());

            if (!ApplicationSettings.isFirstMsgWithLocationTriggered(context)) {
                ApplicationSettings.setFirstMsgWithLocationTriggered(context, true);
            }
        } else if(intent.getAction().equals(Intents.SEND_ALERT_ACTION_SINGLE)) {
            Log.e(">>>>>>>>", "alert update(single) received in AlarmReceiver at current time in millis = " + System.currentTimeMillis() % 100000);

            if (!ApplicationSettings.isFirstMsgWithLocationTriggered(context)) {
                getPanicMessage(mContext).send(getCurrentBestLocation());
                ApplicationSettings.setFirstMsgWithLocationTriggered(context, true);
            }
        }
    }

    PanicMessage getPanicMessage(Context context) {
        return new PanicMessage(mContext);
    }

    Location getCurrentBestLocation() {
        return ApplicationSettings.getCurrentBestLocation(mContext);
    }
}
