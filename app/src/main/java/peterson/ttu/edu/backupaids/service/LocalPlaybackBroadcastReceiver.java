package peterson.ttu.edu.backupaids.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A BroadcastReceiver for local play notifications (have to be separate from remote ones because
 * a local and remote could be running at once and Android makes it hard to distinguish between
 * broadcasts).
 */
public class LocalPlaybackBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "LocalPlaybackReceiver";
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
