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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import peterson.ttu.edu.backupaids.util.ConnectionType;
import peterson.ttu.edu.backupaids.util.Util;

/**
 * Handles connecting to another phone. This class is expected to be fairly transient: you should
 * only construct it when you're ready to look for connections, and then call close() and
 * drop your reference to it when you're done.
 */
public class WiFiConnectionMaker extends BroadcastReceiver
        implements ConnectionMaker, WifiP2pManager.DnsSdTxtRecordListener, WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "ConnectionMaker";

    private int listenPortNumber;

    private final Context context;
    private final WifiP2pManager wifiP2pManager;
    private final ConnectionType mode;
    private static WifiP2pManager.Channel channel;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private static final WifiP2pManager.ActionListener noOpActionListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {}
        @Override
        public void onFailure(int i) {}
    };

    // Stuff for making a connection (peer devices we find, etc.)
    private final Map<String, String> fullPeerBuddyMap = new HashMap<>();
    private final Map<String, WifiP2pDevice> buddyNameToDeviceMap = new HashMap<>();
    private final Map<String, Integer> buddyNameToPortNumber = new HashMap<>();
    // Save IPs so we can re-connect
    private final Map<String, InetAddress> savedIPs = new HashMap<>();
    private int portToConnectTo;

    // Because of the way service discovery appears to work (only one of the two sides gets notified about a service),
    // both sides have to listen for a connection
    private ServerSocket serverSocket;

    // A socket that's set up from the other side and is waiting on us to receive
    private Socket waitingSocket;

    private WifiP2pServiceInfo serviceInfo;

    private WifiP2pDnsSdServiceRequest serviceRequest;

    private ConnectionListener connectionListener;

    /**
     * Set things up and kick things off.
     *
     * @param context Context (Activity) to use for registration
     */
    /* package */ WiFiConnectionMaker(final Context context, ConnectionListener connectionListener, ConnectionType mode) {
        this.context = context;
        this.connectionListener = connectionListener;
        this.mode = mode;

        // Create the P2P manager.
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if ( channel == null) {
            //noinspection ConstantConditions
            channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        }
        context.registerReceiver(this, Util.WIFI_P2P_INTENT_FILTER);

        listenForConnections();
        publishService();
        beginServiceDiscovery();
    }

    public void changeConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public void close() {
        // Only do this once (probably won't hurt to do it again, but it wastes time/energy)
        if ( isClosed.getAndSet(true)) {
            // Already closed
            return;
        }

        if ( serviceRequest != null) {
            wifiP2pManager.removeServiceRequest(channel, serviceRequest, noOpActionListener);
        }

        if ( serviceInfo != null) {
            wifiP2pManager.removeLocalService(channel, serviceInfo, noOpActionListener);
            serviceInfo = null;
        }

        stopListeningForConnections();
        context.unregisterReceiver(this);

        // Due to trouble with connection info not being torn down completely (and thus not being
        // able to re-connect), close out everything we can
        wifiP2pManager.stopPeerDiscovery(channel, noOpActionListener);
        wifiP2pManager.cancelConnect(channel, noOpActionListener);
        wifiP2pManager.clearLocalServices(channel, noOpActionListener);
        wifiP2pManager.clearServiceRequests(channel, noOpActionListener);
        wifiP2pManager.removeGroup(channel, noOpActionListener);

        connectionListener.servicesStopped();
    }

    @Override
    public void readyForInputStream(InputStreamHandler handler) throws IOException {
        handler.inputStreamReady(waitingSocket.getInputStream());

        // This call returning means that the caller is done with the socket
        connectionListener.connectionClosed();
    }

    @Override
    public void readyForOutputStream(OutputStreamHandler handler) throws IOException {
        handler.outputStreamReady(waitingSocket.getOutputStream());

        // This call returning means that the caller is done with the socket
        connectionListener.connectionClosed();    }

    /**
     * Publish our service
     */
    private void publishService() {
        if ( serviceInfo != null) {
            throw new RuntimeException("Cannot publish a service twice!");
        }
        // Taken from https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html
        Map<String, String> record = new HashMap<>();
        record.put(Util.LISTEN_PORT_STRING, Integer.toString(listenPortNumber));
        record.put(Util.BUDDY_NAME_STRING, mode.getMyService());
        record.put(Util.PIN_NUMBER_STRING, Integer.toString(ConnectionMaker.OUR_PIN));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transport layer , and the map containing
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
                connectionListener.servicePublishingFailed();
            }
        });
    }

    /**
     * Call when you want to find peers
     */
    private void beginServiceDiscovery() {
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
                connectionListener.servicesStarted();
            }

            @Override
            public void onFailure(int code) {
                // TODO: better error handling here...
                Log.e(TAG, "Unable to discover services: " + code);
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
        if ( (record.get(Util.BUDDY_NAME_STRING) != null) &&
             (record.get(Util.PIN_NUMBER_STRING) != null) &&
              record.get(Util.BUDDY_NAME_STRING).equals(mode.getOtherService())) {
            Log.i(TAG, "Service available on " + wifiP2pDevice.deviceName + "!");
            fullPeerBuddyMap.put(wifiP2pDevice.deviceAddress, record.get(Util.PIN_NUMBER_STRING));
            buddyNameToPortNumber.put(record.get(Util.PIN_NUMBER_STRING), Integer.valueOf(record.get(Util.LISTEN_PORT_STRING)));
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

    private void listenForConnections() {
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
            if ( connectionListener != null) {
                connectionListener.connectionFailed(e);
            }
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Do this until we're not listening for connections anymore
                    while ( serverSocket != null) {
                        Log.i(TAG, "Listening for connections.");
                        waitingSocket = serverSocket.accept();
                        if ( serverSocket == null) {
                            // We're not listening anymore, so stop
                            try {
                                waitingSocket.close();
                            } catch ( Exception e) {
                                // Do Nothing
                            } finally {
                                waitingSocket = null;
                            }
                            break;
                        }
                        Log.i(TAG, "Accepted connection from " + waitingSocket.getInetAddress().getCanonicalHostName());
                        // TODO: do some sort of security verification (send a code phrase first, perhaps)
                        try {
                            ReadyConnectionMaker.setReadyConnectionMaker(WiFiConnectionMaker.this);
                            connectionListener.connectionReady();
                        } catch ( IOException e) {
                            // Nothing to do here, since all we (might have) done is close the socket
                            Log.w(TAG, "Connection closed/failed: " + e.getMessage(), e);
                        }
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

    private void stopListeningForConnections() {
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
            connectionListener.foundAPeer(remoteAppInstanceName);
            return;
        }

        // We don't know the IP address of the peer, so get it...
        savedIPs.put(remoteAppInstanceName, null);
            // Make the connection
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
                Log.e(TAG, "Connection setup failed: " + i);
                connectionListener.findingPeerFailed(new IOException("WiFiP2pManager.connect failed: " + i));
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
        // Called when we have connection info (in particular, the all-important IP address)

        // Apparently only the group owner can connect to members (not the other way around),
        // so don't do anything if we've found the owner
        if ( wifiP2pInfo.isGroupOwner) {
            return;
        }

        // See if we're waiting on this information, then save it
        for (String buddyName : savedIPs.keySet()) {
            if (savedIPs.get(buddyName) == null) {
                savedIPs.put(buddyName, wifiP2pInfo.groupOwnerAddress);

                // We have all we need to connect at this point. NOW inform the activity
                connectionListener.foundAPeer(buddyName);
                break;
            }
        }
    }

    @Override
    public void makeConnection(final String remoteAppInstanceName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final InetAddress connectionAddress = savedIPs.get(remoteAppInstanceName);
                if ( connectionAddress == null) {
                    // Shouldn't happen since we won't pass connection info without having this...
                    throw new RuntimeException("Don't have connection info for " + remoteAppInstanceName);
                }
                Log.i(TAG, "Connecting to " + connectionAddress + ": " + portToConnectTo);

                try{
                    waitingSocket = new Socket(connectionAddress, portToConnectTo);
                    Log.i(TAG, "Connected!");
                    ReadyConnectionMaker.setReadyConnectionMaker(WiFiConnectionMaker.this);
                    connectionListener.connectionReady();
                } catch ( IOException e) {
                    Log.e(TAG, "Connection failure: ", e);
                    connectionListener.connectionFailed(e);
                }
            }
        }).start();
    }
}
