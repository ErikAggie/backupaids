package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;

import peterson.ttu.edu.backupaids.service.BaseStreamService;
import peterson.ttu.edu.backupaids.service.StreamSoundService;
import peterson.ttu.edu.backupaids.util.ConnectionType;

public class SpeakConnectionController extends ConnectionController {

    public SpeakConnectionController(Context context, Activity activity, Listener listener) {
        super(context, activity, ConnectionType.SPEAK, listener);
    }

    @Override
    public void connectionReady(int connectionNumber) {
        Intent startIntent = new Intent(context, StreamSoundService.class);
        startIntent.getExtras().putInt(BaseStreamService.CONNECTION_NUMBER_EXTRA, connectionNumber);
        activity.startService(startIntent);
    }

    @Override
    protected void stopActivity() {
        activity.stopService(new Intent(context, StreamSoundService.class));
    }
}
