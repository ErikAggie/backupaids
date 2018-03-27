package peterson.ttu.edu.backupaids.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;

/**
 * Handles connecting to another phone
 */
public class ConnectionManager extends BroadcastReceiver
        implements WifiP2pManager.DnsSdTxtRecordListener, WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "ConnectionManager";
    protected int listenPortNumber;

    protected final Context context;
    protected final ConnectionListener listener;
    protected final WifiP2pManager wifiP2pManager;
    protected static WifiP2pManager.Channel channel;

    // Stuff for making a connection (peer devices we find, etc.)
    private final Map<String, String> fullPeerBuddyMap = new HashMap<>();
    private final Map<String, WifiP2pDevice> buddyNameToDeviceMap = new HashMap<>();
    private final Map<String, Integer> buddyNameToPortNumber = new HashMap<>();
    // Save IPs so we can re-connect to an existing guy
    private static final Map<String, InetAddress> savedIPs = new HashMap<>();
    private int portToConnectTo;

    // Because of the way service discovery appears to work (only one of the two sides gets notified about a service),
    // both sides have to listen for a connection
    private ServerSocket serverSocket;

    private WifiP2pServiceInfo serviceInfo;

    private WifiP2pDnsSdServiceRequest serviceRequest;

    /**
     * Set things up.
     * @param context Context (Activity) to use for registration
     * @param listener Where to send event notifications
     */
    public ConnectionManager(final Context context, final ConnectionListener listener) {
        this.context = context;
        this.listener = listener;

        // Create the P2P manager. Only initialize it once
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if ( channel == null) {
            channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        }
        context.registerReceiver(this, Util.WIFI_P2P_INTENT_FILTER);

        listenForConnections();
        publishService();
        beginServiceDiscovery();
    }

    public void close() {
        stopServiceDiscovery();
        unpublishService();
        stopListeningForConnections();
        context.unregisterReceiver(this);
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
                listener.servicesStarted();
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
                    listener.servicesStopped();
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
            }

            @Override
            public void onFailure(int i) {
                // Don't care
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
            } else {
                Log.i(TAG, "Wifi P2P disabled!");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "Peers changed...");
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
    public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice wifiP2pDevice) {
        // We've found a service and we need to see if it's our app running on a different phone
        // TODO: the name can include a "security key" (i.e. the random number) that we could use to validate the connection
        if ( (record.get(Util.BUDDY_NAME_STRING) != null) &&
              record.get(Util.BUDDY_NAME_STRING).startsWith(context.getString(R.string.app_name))) {
            Log.i(TAG, "Service available on " + wifiP2pDevice.deviceName + "!");
            fullPeerBuddyMap.put(wifiP2pDevice.deviceAddress, record.get(Util.BUDDY_NAME_STRING));
            buddyNameToPortNumber.put(record.get(Util.BUDDY_NAME_STRING), Integer.valueOf(record.get(Util.LISTEN_PORT_STRING)));
            for ( String key : record.keySet()) {
                Log.d(TAG, key + ":" + record.get(key));
            }

        }
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice wifiP2pDevice) {
        if ( instanceName.equals(Util.SERVICE_NAME)) {
            Log.i(TAG, "Found a compatriot! " + wifiP2pDevice.deviceName + " at " + wifiP2pDevice.deviceAddress);
            if ( fullPeerBuddyMap.containsKey(wifiP2pDevice.deviceAddress)) {
                String remoteAppInstanceName = fullPeerBuddyMap.get(wifiP2pDevice.deviceAddress);
                Log.i(TAG, "Found a compatriot! " + wifiP2pDevice.deviceName);
                buddyNameToDeviceMap.put(remoteAppInstanceName, wifiP2pDevice);

                // Get the connection information so we can be sure we're ready to go
                getConnectionInfo(remoteAppInstanceName);
            }
        }
    }

    //--------------------------------------------------------------------------------------------
    // Methods for listening for connections
    //--------------------------------------------------------------------------------------------

    protected void listenForConnections() {
        if ( serverSocket != null) {
            try {
                serverSocket.close();
            } catch ( IOException e) {
                // Nothing we can do...
            } finally {
                serverSocket = null;
            }
        }
        try {
            serverSocket = new ServerSocket(0);
            listenPortNumber = serverSocket.getLocalPort();
        } catch ( IOException e) {
            Log.e(TAG, "Error creating server socket: " + e.getMessage(), e);
            listener.connectionFailed(e);
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Do this until we're not listening for connections anymore
                    while ( serverSocket != null) {
                        Log.i(TAG, "Listening for connections.");
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
                    listener.connectionReady(clientSocket);
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
                    listener.connectionClosed();
                }
            }
        }).start();
    }

    protected void stopListeningForConnections() {
        if ( serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                serverSocket = null;
            }
        }
        Log.i(TAG, "Not listening for connections.");
    }

    //---------------------------------------------------------------------------------------------
    // Methods for making a connection
    //---------------------------------------------------------------------------------------------

    /**
     * Connect to the specified app instance
     *
     * @param remoteAppInstanceName App instance to connect to
     */
    private void getConnectionInfo(String remoteAppInstanceName) {
        // Have to do a reverse lookup to get the
        final WifiP2pDevice device = buddyNameToDeviceMap.get(remoteAppInstanceName);
        portToConnectTo = buddyNameToPortNumber.get(remoteAppInstanceName);

        if ( savedIPs.containsKey(remoteAppInstanceName) && savedIPs.get(remoteAppInstanceName) != null) {
            // We already know the IP and can connect directly!
            listener.foundAPeer(remoteAppInstanceName);
            return;
        }

        // We don't know the IP address of the peer, so get it...
        savedIPs.put(remoteAppInstanceName, null);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Connection initiated!");
            }

            @Override
            public void onFailure(int i) {
                Log.e(TAG, "Connection failed: " + i);
                listener.connectionFailed(new IOException("WiFiP2pManager.connect failed: " + i));
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
        // Called when we have connection info (in particular, the all-important IP address)

        // See if we're waiting on this information, then save it
        for (String buddyName : savedIPs.keySet()) {
            if (savedIPs.get(buddyName) == null) {
                savedIPs.put(buddyName, wifiP2pInfo.groupOwnerAddress);

                // We have all we need to connect at this point. NOW inform the activity
                listener.foundAPeer(buddyName);
                break;
            }
        }

    }

    public void makeConnection(String remoteAppInstanceName) {

        final InetAddress connectionAddress = savedIPs.get(remoteAppInstanceName);
        if ( connectionAddress == null) {
            // Shouldn't happen since we won't pass connection info without having this...
            throw new RuntimeException("Don't have connection info for " + remoteAppInstanceName);
        }

        // Do this on a separate thread so we don't block the main thread
        new Thread(new Runnable() {
            public void run() {
                Log.i(TAG, "Connecting to " + connectionAddress + ": " + portToConnectTo);
                try (Socket client = new Socket(connectionAddress, portToConnectTo)){
                    Log.i(TAG, "Connected!");
                    listener.connectionReady(client);
                } catch ( IOException e) {
                    Log.e(TAG, "Connection failure: ", e);
                    listener.connectionFailed(e);
                } finally {
                    Log.i(TAG, "Connection closed");
                    listener.connectionClosed();
                }
            }
        }).start();
    }


}
