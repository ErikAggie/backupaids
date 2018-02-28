package peterson.ttu.edu.backupaids.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens for connections to this app from other headsets wanting to send data
 */

public class ConnectionManager extends BroadcastReceiver
        implements WifiP2pManager.PeerListListener, WifiP2pManager.DnsSdTxtRecordListener, WifiP2pManager.DnsSdServiceResponseListener {

    private static final String TAG = "ConnectionManager";

    private final AppCompatActivity activity;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;

    private WifiP2pDnsSdServiceRequest serviceRequest;

    private final Map<String, String> peerBuddyMap = new HashMap<>();

    public ConnectionManager(final AppCompatActivity activity) {
        this.activity = activity;

        // Create the P2P manager
        wifiP2pManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(activity, activity.getMainLooper(), null);

        // Taken from https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html
        Map record = new HashMap();
        record.put("listenport", Integer.toString(57364));
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

    /**
     * Call when you want to find peers
     */
    public void beginServiceDiscovery() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Nothing to do...
            }

            @Override
            public void onFailure(int i) {
                // TODO: create better error handling
                Log.e(TAG,"Peer discovery is not working!");
            }
        });
        wifiP2pManager.setDnsSdResponseListeners(channel, this, this);

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wifiP2pManager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
            }

            @Override
            public void onFailure(int code) {
                // TODO: better error handling here...
                Log.e(TAG, "Creating service request failed! " + code);
            }
        });

        wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int code) {
                // TODO: better error handling here...
                Log.e(TAG, "Unable to discover services: " + code);
            }
        });
    }

    /**
     * Call when you want to stop finding peers (i.e. when an activity is paused)
     */
    public void stopServiceDiscovery() {
        if ( serviceRequest != null) {
            wifiP2pManager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Cool
                }

                @Override
                public void onFailure(int i) {
                    // Nothing we can do, really
                }
            });
        }
        wifiP2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Don't care...
            }

            @Override
            public void onFailure(int i) {
                // Don't care...
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

            // Request list of peers
            if ( wifiP2pManager != null) {
                wifiP2pManager.requestPeers(channel, this);
            }

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

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        List<WifiP2pDevice> peers = new ArrayList<>();
        peers.addAll(peerList.getDeviceList());
        for ( WifiP2pDevice device : peers) {
            Log.w(TAG, "Peer device: " + device.deviceName);

        }
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice wifiP2pDevice) {
        Log.w(TAG, "Service available on " + wifiP2pDevice.deviceName + "!");
        peerBuddyMap.put(wifiP2pDevice.deviceAddress, record.get("buddyName"));
        for ( String key : record.keySet()) {
            Log.i(TAG, key + ":" + record.get(key));
        }
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice wifiP2pDevice) {
        // Update the device name with the human-friendly version from
        // the DnsTxtRecord, assuming one arrived.
        wifiP2pDevice.deviceName = peerBuddyMap
                .containsKey(wifiP2pDevice.deviceAddress) ? peerBuddyMap
                .get(wifiP2pDevice.deviceAddress) : wifiP2pDevice.deviceName;

        // Add to the custom adapter defined specifically for showing
        // wifi devices.
        /*WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                .findFragmentById(R.id.frag_peerlist);
        WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                .getListAdapter());

        adapter.add(resourceType);
        adapter.notifyDataSetChanged();*/
        Log.i(TAG, "onBonjourServiceAvailable " + instanceName);

    }
}
