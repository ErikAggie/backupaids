package peterson.ttu.edu.backupaids.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import peterson.ttu.edu.backupaids.util.ConnectionType;

public class BluetoothConnectionMaker implements ConnectionMaker {
    private static final String TAG = "BTConnectionMaker";

    private final Context context;

    private final Map<String, BluetoothDevice> bluetoothDeviceMap = new HashMap<>();

    private final BluetoothAdapter bluetoothAdapter;

    private ConnectionListener connectionListener;

    // TODO: allow user to pass along a previous connection

    /* package */ BluetoothConnectionMaker(@NonNull Context context, ConnectionListener connectionListener, ConnectionType connectionType) throws IOException {
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
            throw new BluetoothNotEnabledException("Bluetooth is not enabled; kindly ask the user to enable Bluetooth.");
        }

        // Get the list of paired devices and compare it to our saved list
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                Log.i(TAG, "Paired device: " + device.getName() + ", " + device.getAddress() + ", " + device.getBluetoothClass());
            }
        }

        // TODO: only do this if the user didn't request a specific connection...
        // TODO: and only if speaking...
        discoverConnections();
    }

    private void discoverConnections() {

        // Make us discoverable...
//        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        context.startActivity(discoverableIntent);

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
        }
    }

    @Override
    public void changeConnectionListener(ConnectionListener connectionListener) {
    }

    @Override
    public void close() {
        bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void makeConnection(String remoteAppInstanceName) {

    }

    @Override
    public void readyForInputStream(InputStreamHandler handler) throws IOException {

    }

    @Override
    public void readyForOutputStream(OutputStreamHandler handler) throws IOException {

    }
}
