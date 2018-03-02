package peterson.ttu.edu.backupaids.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peterson.ttu.edu.backupaids.R;

/**
 * Listens for connections to this app from other headsets wanting to send data
 */

public class ConnectionManager extends BroadcastReceiver
        implements WifiP2pManager.PeerListListener, WifiP2pManager.DnsSdTxtRecordListener, WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "ConnectionManager";
    private static final String SERVICE_NAME = "ListenForExternalMic";
    private static final String BUDDY_NAME_STRING = "buddyname";
    // TODO: need to figure out how to make this adaptive (take a port that's free)
    private static final int CONNECTION_PORT = 57364;

    private final AppCompatActivity activity;
    private final ConnectionListener connectionListener;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;

    private WifiP2pDnsSdServiceRequest serviceRequest;
    private ServerSocket serverSocket;

    private final Map<String, String> fullPeerBuddyMap = new HashMap<>();
    private final Map<String, WifiP2pDevice> buddyNameToDeviceMap = new HashMap<>();
    private final List<String> buddiesWithOurService = new ArrayList<>();

    public ConnectionManager(final AppCompatActivity activity,
                             ConnectionListener connectionListener) {
        this.activity = activity;
        this.connectionListener = connectionListener;

        // Create the P2P manager
        wifiP2pManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(activity, activity.getMainLooper(), null);

        // Taken from https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html
        Map record = new HashMap();
        record.put("listenport", Integer.toString(CONNECTION_PORT));
        record.put(BUDDY_NAME_STRING, activity.getString(R.string.app_name) + (int) (Math.random() * 1000));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, "_presence._tcp", record);

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
            public void onSuccess() { }

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

    public void connect(String remoteAppInstanceName) {
        // Have to do a reverse lookup to get the
        final WifiP2pDevice device = buddyNameToDeviceMap.get(remoteAppInstanceName);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Connection successful!");
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(activity, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void listenForConnections() {
        if ( serverSocket != null) {
            throw new RuntimeException("Cannot listen for connections twice!");
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    serverSocket = new ServerSocket(CONNECTION_PORT);
                    // Do this until we're not listening for connections anymore
                    while ( serverSocket != null) {
                        Socket clientSocket = serverSocket.accept();
                        if ( serverSocket == null) {
                            // We're not listening anymore, so stop
                            try {
                                clientSocket.close();
                            } catch ( Exception e) {

                            }
                            break;
                        }
                        handleSocket(clientSocket);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Connection listening stopped: " + e.getMessage(), e);
                    e.printStackTrace();
                } finally {
                    if ( serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            // Don't care
                        } finally {
                            serverSocket = null;
                        }
                    }
                }
            }
        }).start();
    }

    private void handleSocket(final Socket clientSocket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Accepted connection from " + clientSocket.getInetAddress().getCanonicalHostName());
                // TODO: do some sort of security verification (send a code phrase first, perhaps)
                try {
                    connectionListener.incomingConnection(clientSocket);
                } catch ( IOException e) {
                    // The connection might've been caught elsewhere. This is okay
                    Log.w(TAG, "Connection closed/failed: " + e.getMessage(), e);
                } finally {
                    if ( clientSocket != null) {
                        try {
                            clientSocket.close();
                        } catch (Exception e) {
                            // Do nothing
                        }
                    }
                }
            }
        }).start();
    }

    public void stopListeningForConnections() {
        if ( serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                serverSocket = null;
            }
        }
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
            buddiesWithOurService.clear();
            connectionListener.supportedPeersChanged(buddiesWithOurService);

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
        if ( (record.get(BUDDY_NAME_STRING) != null) &&
              record.get(BUDDY_NAME_STRING).startsWith(activity.getString(R.string.app_name))) {
            Log.i(TAG, "Service available on " + wifiP2pDevice.deviceName + "!");
            fullPeerBuddyMap.put(wifiP2pDevice.deviceAddress, record.get(BUDDY_NAME_STRING));
            for ( String key : record.keySet()) {
                Log.d(TAG, key + ":" + record.get(key));
            }
        }
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice wifiP2pDevice) {
        if ( instanceName.equals(SERVICE_NAME)) {
            Log.i(TAG, "Found a compatriot! " + wifiP2pDevice.deviceName + " at " + wifiP2pDevice.deviceAddress);
            if ( fullPeerBuddyMap.containsKey(wifiP2pDevice.deviceAddress)) {
                String remoteAppInstanceName = fullPeerBuddyMap.get(wifiP2pDevice.deviceAddress);
                Log.i(TAG, "Found a compatriot! " + wifiP2pDevice.deviceName);
                buddiesWithOurService.add(remoteAppInstanceName);
                buddyNameToDeviceMap.put(remoteAppInstanceName, wifiP2pDevice);
                connectionListener.supportedPeersChanged(buddiesWithOurService);
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                // We're ready to send data!
        final InetAddress connectionAddress = wifiP2pInfo.groupOwnerAddress;

        // Do this on a separate thread so we don't block the main thread
        new Thread(new Runnable() {
            public void run() {
                try (Socket client = new Socket(connectionAddress, CONNECTION_PORT)){
                    connectionListener.connectionReady(client);
                } catch ( IOException e) {
                    Log.e(TAG, "Connection failure: " + e.getMessage());
                    connectionListener.connectionFailed(e);
                } finally {
                    connectionListener.connectionClosed();
                }
            }
        }).start();
    }

    public interface ConnectionListener {
        void supportedPeersChanged(List<String> supportedPeers);
        void connectionReady(Socket socket) throws IOException;
        void incomingConnection(Socket socket) throws IOException;
        void connectionFailed(IOException e);
        void connectionClosed();
    }
}
