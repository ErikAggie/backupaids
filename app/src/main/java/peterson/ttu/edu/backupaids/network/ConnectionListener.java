package peterson.ttu.edu.backupaids.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import peterson.ttu.edu.backupaids.Util;

/**
 * Listens for connections to this app from other headsets wanting to send data
 */

public class ConnectionListener extends BroadcastReceiver {

    private static final String TAG = "ConnectionListener";

    private final AppCompatActivity activity;
    private final IntentFilter intentFilter = new IntentFilter();
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;

    public ConnectionListener(final AppCompatActivity activity) {
        this.activity = activity;

        // Create an intent filter for events we need.
        // Taken from https://developer.android.com/training/connect-devices-wirelessly/wifi-direct.html
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        activity.registerReceiver(this, intentFilter);

        // Create the P2P manager
        wifiP2pManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(activity, activity.getMainLooper(), null);

        // Taken from https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html
        Map record = new HashMap();
        record.put("listenport", String.valueOf(Util.LISTENING_PORT));
        record.put("buddyname", "Hearing Phone" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("ListenForExternalMic", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Created local service!");
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
            }

            @Override
            public void onFailure(int arg0) {
                Log.e(TAG, "Failed to set up listener: " + arg0);
                activity.finish();
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Taken from https://developer.android.com/training/connect-devices-wirelessly/wifi-direct.html
        String action = intent.getAction();
        // TODO: this should do more than just log it...
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.i(TAG, "Wifi P2P enabled!");
                //activity.setIsWifiP2pEnabled(true);
            } else {
                Log.i(TAG, "Wifi P2P disabled!");
                //activity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "Peers changed...");

            // The peer list has changed! We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "Connection changed...");

            // Connection state changed! We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "This device changed...");
//            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
//                    .findFragmentById(R.id.frag_list);
//            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
//                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }
}
