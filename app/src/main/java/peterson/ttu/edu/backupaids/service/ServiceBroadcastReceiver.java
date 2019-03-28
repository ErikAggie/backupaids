package peterson.ttu.edu.backupaids.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ServiceBroadcastRcvr";

    /* package */ static final String SERVICE_NAME_EXTRA = "ServiceNameExtra";

    private static final Map<String, Callback> CALLBACK_MAP = new HashMap();

    /* package */ static void registerCallback(String serviceName, Callback callback) {
        CALLBACK_MAP.put(serviceName, callback);
    }

    static void unregisterCallback(String serviceName) {
        CALLBACK_MAP.remove(serviceName);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String serviceName = intent.getStringExtra(SERVICE_NAME_EXTRA);
        if ( serviceName == null) {
            throw new RuntimeException("No service name added to intent!");
        }
        Callback callback = CALLBACK_MAP.get(serviceName);
        if ( callback == null) {
            Log.e(TAG, "Unknown service callback "+ serviceName);
            return;
        }
        callback.stopRequested();
    }

    /* package */ interface Callback {
        void stopRequested();
    }
}
