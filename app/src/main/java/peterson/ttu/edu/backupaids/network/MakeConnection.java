package peterson.ttu.edu.backupaids.network;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peterson.ttu.edu.backupaids.Util;

/**
 * Controller (sort of...) for making a connection
 */
public class MakeConnection extends ConnectionManager {
    private static final String TAG = "MakeConnection";

    private final MakeConnectionListener makeConnectionListener;
    private int connectionNumber = 1;

    private final Map<String, String> fullPeerBuddyMap = new HashMap<>();
    private final Map<String, WifiP2pDevice> buddyNameToDeviceMap = new HashMap<>();
    private final Map<String, Integer> buddyNameToPortNumber = new HashMap<>();
    private final List<String> buddiesWithOurService = new ArrayList<>();

    private int portToConnectTo;
    private boolean tryingToConnect = false;

    /**
     * Set things up and begin service discovery
     *
     * @param context Activity to use as the context
     * @param listener Listener for events
     */
    public MakeConnection(Context context, MakeConnectionListener listener) {
        super(context, listener);
        makeConnectionListener = listener;
        context.registerReceiver(this, Util.WIFI_P2P_INTENT_FILTER);

        publishService();
        beginServiceDiscovery();
    }

    /**
     * Stop everything
     */
    public void close() {
        tryingToConnect = false;
        stopServiceDiscovery();
        unpublishService();
        context.unregisterReceiver(this);
    }

    @Override
    protected void buddyAvailable(Map<String, String> record, WifiP2pDevice wifiP2pDevice) {
        super.buddyAvailable(record, wifiP2pDevice);
        Log.i(TAG, "Service available on " + wifiP2pDevice.deviceName + "!");
        fullPeerBuddyMap.put(wifiP2pDevice.deviceAddress, record.get(Util.BUDDY_NAME_STRING));
        buddyNameToPortNumber.put(record.get(Util.BUDDY_NAME_STRING), Integer.valueOf(record.get(Util.LISTEN_PORT_STRING)));
        for ( String key : record.keySet()) {
            Log.d(TAG, key + ":" + record.get(key));
        }
    }

    @Override
    protected void serviceAvailable(WifiP2pDevice wifiP2pDevice) {
        super.serviceAvailable(wifiP2pDevice);
        if ( fullPeerBuddyMap.containsKey(wifiP2pDevice.deviceAddress)) {
            String remoteAppInstanceName = fullPeerBuddyMap.get(wifiP2pDevice.deviceAddress);
            Log.i(TAG, "Found a compatriot! " + wifiP2pDevice.deviceName);
            buddiesWithOurService.add(remoteAppInstanceName);
            buddyNameToDeviceMap.put(remoteAppInstanceName, wifiP2pDevice);
            makeConnectionListener.supportedPeersChanged(buddiesWithOurService);
        }

    }

    /**
     * Connect to the specified app instance
     *
     * @param remoteAppInstanceName App instance to connect to
     */
    public void connect(String remoteAppInstanceName) {
        // Have to do a reverse lookup to get the
        final WifiP2pDevice device = buddyNameToDeviceMap.get(remoteAppInstanceName);
        portToConnectTo = buddyNameToPortNumber.get(remoteAppInstanceName);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        tryingToConnect = true;

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Connection initiated!");
            }

            @Override
            public void onFailure(int i) {
                Log.e(TAG, "Connection failed: " + i);
                makeConnectionListener.connectionFailed(new IOException("WiFiP2pManager.connect failed: " + i));
            }
        });
    }

    /**
     * Getting here means we're ready to connect
     *
     * @param wifiP2pInfo Info we need to make the socket connection
     */
    protected void gotConnectionInfo(WifiP2pInfo wifiP2pInfo) {
        super.gotConnectionInfo(wifiP2pInfo);
        final int thisConnection = connectionNumber++;
        if ( !tryingToConnect) {
            return;
        }
        tryingToConnect = false;

        // We're ready to send data!
        final InetAddress connectionAddress = wifiP2pInfo.groupOwnerAddress;


        // Do this on a separate thread so we don't block the main thread
        new Thread(new Runnable() {
            public void run() {
                try (Socket client = new Socket(connectionAddress, portToConnectTo)){
                    Log.i(TAG, "Connection " + thisConnection + " connected!");
                    makeConnectionListener.connectionReady(client);
                } catch ( IOException e) {
                    Log.e(TAG, "Connection " + thisConnection + " failure: ", e);
                    makeConnectionListener.connectionFailed(e);
                } finally {
                    Log.i(TAG, "Connection " + thisConnection + " closed");
                    makeConnectionListener.connectionClosed();
                }
            }
        }).start();
    }

}
