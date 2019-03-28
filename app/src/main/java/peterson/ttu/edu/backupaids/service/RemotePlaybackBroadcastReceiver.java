package peterson.ttu.edu.backupaids.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RemotePlaybackBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "RemotePlaybackReceiver";
    private static ServiceNotificationCallback savedCallback;

    /* package */ static void registerCallback(ServiceNotificationCallback callback) {
        savedCallback = callback;
    }

    static void unregisterCallback() {
        savedCallback = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ( savedCallback == null) {
            Log.e(TAG, "No service callback registered!");
        }
        savedCallback.stopRequested();
    }
}
