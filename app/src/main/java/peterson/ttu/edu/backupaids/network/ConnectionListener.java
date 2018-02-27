package peterson.ttu.edu.backupaids.network;

import android.content.Context;
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

public class ConnectionListener {

    private static final String TAG = "ConnectionListener";

    private final AppCompatActivity activity;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;

    public ConnectionListener(final AppCompatActivity activity) {
        this.activity = activity;

        // Create the P2P manager
        wifiP2pManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(activity, activity.getMainLooper(), null);

        // Taken from https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html
        Map record = new HashMap();
        record.put("listenport", String.valueOf(Util.LISTENING_PORT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
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
}
