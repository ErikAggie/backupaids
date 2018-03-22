package peterson.ttu.edu.backupaids.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;

/**
 * Base class for connecting to another phone
 */
public class ConnectionManager extends BroadcastReceiver
        implements WifiP2pManager.PeerListListener, WifiP2pManager.DnsSdTxtRecordListener, WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "ConnectionManager";
    protected int listenPortNumber;

    protected final Context context;
    private final ConnectionBaseListener listener;
    protected final WifiP2pManager wifiP2pManager;
    protected final WifiP2pManager.Channel channel;

    private WifiP2pServiceInfo serviceInfo;

    private WifiP2pDnsSdServiceRequest serviceRequest;

    /**
     * Set things up. Protected because only a sub-class should instantiate this
     * @param context
     * @param listener
     */
    protected ConnectionManager(final Context context, final ConnectionBaseListener listener) {
        this.context = context;
        this.listener = listener;

        // Create the P2P manager
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
    }

    /**
     * Publish our service
     */
    protected void publishService() {
        if ( serviceInfo != null) {
            throw new RuntimeException("Cannot publish a service twice!");
        }
        // Taken from https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html
        Map<String, String> record = new HashMap<>();
        record.put(Util.LISTEN_PORT_STRING, Integer.toString(listenPortNumber));
        record.put(Util.BUDDY_NAME_STRING, context.getString(R.string.app_name) + (int) (Math.random() * 1000));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(Util.SERVICE_NAME, "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Created local service!");
            }

            @Override
            public void onFailure(int arg0) {
                Log.e(TAG, "Failed to set up listener: " + arg0);
                listener.servicePublishingFailed();
            }
        });
    }

    /**
     * Unpublish our service
     */
    protected void unpublishService() {
        if ( serviceInfo == null) {
            // Nothing to unpublish
            return;
        }
        wifiP2pManager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Good, but not something we need to react to...
            }

            @Override
            public void onFailure(int i) {
                // Nothing we can do about it...
            }
        });
        serviceInfo = null;
    }

    /**
     * Call when you want to find peers
     */
    protected void beginServiceDiscovery() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                listener.servicesStarted();
                Log.i(TAG, "Discover peers successful");
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
                Log.i(TAG,"Add service request successful");
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
                Log.i(TAG, "Discover services succeeded.");
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
    protected void stopServiceDiscovery() {
        Log.i(TAG, "Stopping service discovery");
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
                listener.servicesStopped();
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
            // Do not clear the buddy list as the screen shouldn't be up long enough for this to grow very stale

            // Request list of peers
            if ( wifiP2pManager != null) {
                wifiP2pManager.requestPeers(channel, this);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "Connection changed...");

            if (wifiP2pManager == null) {
                return;
            }

            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                wifiP2pManager.requestConnectionInfo(channel, this);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "This device changed...");
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
        // TODO: the name can include a "security key" (i.e. the random number) that we could use to validate the connection
        if ( (record.get(Util.BUDDY_NAME_STRING) != null) &&
              record.get(Util.BUDDY_NAME_STRING).startsWith(context.getString(R.string.app_name))) {
            buddyAvailable(record, wifiP2pDevice);
        }
    }

    /**
     * Override if you want to know about "buddies"
     * @param record Record sent from the other app
     * @param device Device information
     */
    protected void buddyAvailable(Map<String, String> record, WifiP2pDevice device) {}

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice wifiP2pDevice) {
        if ( instanceName.equals(Util.SERVICE_NAME)) {
            Log.i(TAG, "Found a compatriot! " + wifiP2pDevice.deviceName + " at " + wifiP2pDevice.deviceAddress);
            serviceAvailable(wifiP2pDevice);
        }
    }

    /**
     * Override if you want to know when a service is available
     * @param wifiP2pDevice Device that's ready to connect to
     */
    protected void serviceAvailable(WifiP2pDevice wifiP2pDevice) {}

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
        gotConnectionInfo(wifiP2pInfo);
    }

    /**
     * Override if you want to know when we have connection info (i.e. we're ready to connect)
     * @param wifiP2pInfo Connection info
     */
    protected void gotConnectionInfo(WifiP2pInfo wifiP2pInfo) {}
}
