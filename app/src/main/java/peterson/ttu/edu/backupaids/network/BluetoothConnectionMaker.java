package peterson.ttu.edu.backupaids.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.model.DeviceInfo;
import peterson.ttu.edu.backupaids.model.DeviceInfoManager;
import peterson.ttu.edu.backupaids.util.ConnectionType;
import peterson.ttu.edu.backupaids.util.Util;

public class BluetoothConnectionMaker implements ConnectionMaker {
    private static final String TAG = "BTConnectionMaker";

    private final Context context;

    private final Map<String, BluetoothDevice> bluetoothDeviceMap = new HashMap<>();

    private final BluetoothAdapter bluetoothAdapter;

    private ConnectionListener connectionListener;

    private BluetoothServerSocket serverSocket;
    private BluetoothSocket waitingSocket;

    /**
     * Constructor. Package-private since this should be factory-built
     * @param context Context
     * @param connectionListener Connection listener
     * @param connectionType Speak or listen
     * @param existingDevice If non-null, signals a desire to reconnect. For listening, this is just
     *                       an indication that we want to listen for pairing requests (i.e. listen
     *                       for socket connections but don't make us discoverable)
     * @throws IOException If the device doesn't support Bluetooth or some other exception
     */
    /* package */ BluetoothConnectionMaker(@NonNull Context context,
                                           @NonNull ConnectionListener connectionListener,
                                           @NonNull ConnectionType connectionType,
                                           DeviceInfo existingDevice) throws IOException {
        this.context = context;
        this.connectionListener = connectionListener;

        // Make sure we support Bluetooth and that it's turned on
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            throw new IOException("This device doesn't support Bluetooth; sending/receiving data will not work.");
        }
        if (!bluetoothAdapter.isEnabled()) {
            // This exception is designed to have the caller (or, rather, the UI element behind the controller)
            // request Bluetooth be enabled, after which they should start the process again
            throw new BluetoothNotEnabledException();
        }

        if ( connectionType == ConnectionType.SPEAK) {
            if ( existingDevice == null) {
                discoverConnections();
            } else {
                BluetoothDevice bluetoothDevice = DeviceInfoManager.findBluetoothDevice(existingDevice);
                if ( bluetoothDevice == null) {
                    throw new IOException("Couldn't find bluetooth device for " + existingDevice.getName());
                }
                makeConnection(bluetoothDevice);
            }
        } else {
            if ( existingDevice == null) {
                // Listen for discoverable updates
                connectionListener.nowDiscoverable();
                listenForDiscoveryEnding();
            }
            listenForConnections();
        }
    }

    @Override
    public boolean isConnected() {
        return waitingSocket != null;
    }

    private void discoverConnections() {
        bluetoothDeviceMap.clear();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch ( intent.getAction()) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if ( device == null || device.getName() == null) {
                            break;
                        }
                        Log.i(TAG, "Found " + device.getName());
                        if ( bluetoothDeviceMap.containsKey(device.getName())) {
                            Log.w(TAG, "Duplicated Bluetooth name: " + device.getName());
                        }
                        bluetoothDeviceMap.put(device.getName(), device);
                        connectionListener.foundAPeer(device.getName());
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.i(TAG, "Discovery finished!");
                        context.unregisterReceiver(this);
                        connectionListener.discoveryFinished();
                        break;
                    default:
                        Log.w(TAG, "Got action " + action);
                        break;
                }
            }
        };

        context.registerReceiver(receiver, filter);
        if ( bluetoothAdapter.startDiscovery()) {
            Log.i(TAG, "Scanning for devices...");
            connectionListener.discoveryStarted();
        } else {
            Log.w(TAG, "Discovery process didn't start!");
            //
            connectionListener.connectionDiscoveryFailed();
        }
    }

    private void listenForDiscoveryEnding() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch ( intent.getAction()) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        Log.i(TAG, "Connected!");
                        context.unregisterReceiver(this);
                        break;
                    case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                        int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1000);
                        Log.i(TAG, "Scan mode now " + scanMode);
                        if ( scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE) {
                            // We are no longer discoverable
                            context.unregisterReceiver(this);
                            connectionListener.connectionDiscoveryFailed();
                        }
                        break;
                    default:
                        Log.w(TAG, "Got action " + action);
                        break;
                }
            }
        };

        context.registerReceiver(receiver, filter);
    }

    @Override
    public void changeConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public void close() {
        bluetoothAdapter.cancelDiscovery();
        stopListeningForConnections();
    }

    @Override
    public void makeConnection(String remoteAppInstanceName) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice serverDevice = bluetoothDeviceMap.get(remoteAppInstanceName);
        if (serverDevice == null) {
            throw new RuntimeException("Attempt to connect to a device we didn't find!");
        }

        makeConnection(serverDevice);
    }

    private void makeConnection(BluetoothDevice serverDevice) {
        try {
            waitingSocket = serverDevice.createRfcommSocketToServiceRecord(UUID.fromString(Util.UUID_STRING));
            ReadyConnectionMaker.setReadyConnectionMaker(BluetoothConnectionMaker.this);
            waitingSocket.connect();
            DeviceInfoManager.getInstance(context).addDevice(new DeviceInfo(serverDevice.getName(), serverDevice.getAddress()));
            connectionListener.connectionReady();
        } catch ( IOException e) {
            connectionListener.connectionFailed(e);
        }
    }

    @Override
    public void readyForInputStream(InputStreamHandler handler) throws IOException {
        try {
            handler.inputStreamReady(waitingSocket.getInputStream());
        } finally {
            // This call returning means that the caller is done with the socket
            try {
                waitingSocket.close();
            } catch ( Exception e) {

            }
            waitingSocket = null;
            connectionListener.connectionClosed();
        }
    }

    @Override
    public void readyForOutputStream(OutputStreamHandler handler) throws IOException {
        try {
            handler.outputStreamReady(waitingSocket.getOutputStream());
        } finally {
            // This call returning means that the caller is done with the socket
            try {
                waitingSocket.close();
            } catch ( Exception e) {

            }
            waitingSocket = null;
            connectionListener.connectionClosed();
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
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(context.getString(R.string.app_name), UUID.fromString(Util.UUID_STRING));
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
                            // We're not listening anymore, so stopRequested
                            try {
                                waitingSocket.close();
                            } catch ( Exception e) {
                                // Do Nothing
                            } finally {
                                waitingSocket = null;
                            }
                            break;
                        }
                        // No need to listen further
                        serverSocket.close();
                        serverSocket = null;
                        BluetoothDevice remoteDevice = waitingSocket.getRemoteDevice();
                        Log.i(TAG, "Accepted connection from " + remoteDevice.getName());

                        // Add this device so we can quickly connect to it later
                        DeviceInfoManager.getInstance(context).addDevice(new DeviceInfo(remoteDevice.getName(), remoteDevice.getAddress()));

                        // Connection is ready to use
                        ReadyConnectionMaker.setReadyConnectionMaker(BluetoothConnectionMaker.this);
                        connectionListener.connectionReady();
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

}
