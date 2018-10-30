package peterson.ttu.edu.backupaids.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.Set;

import peterson.ttu.edu.backupaids.util.ConnectionType;

public class BluetoothConnectionMaker implements ConnectionMaker {
    private static final String TAG = "BTConnectionMaker";

    private final Context context;

    /* package */ BluetoothConnectionMaker(@NonNull Context context, ConnectionListener connectionListener, ConnectionType connectionType) throws IOException {
        this.context = context;

        // Make sure we support Bluetooth and that it's turned on
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            throw new IOException("This device doesn't support Bluetooth; sending/receiving data will not work.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            // This exception is designed to have the caller (or, rather, the UI element behind the controller)
            // request Bluetooth be enabled, after which they should start the process again
            throw new BluetoothNotEnabledException("Bluetooth is not enabled; kindly ask the user to enable Bluetooth.");
        }

        // Get the list of paired devices and compare it to our saved list
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                Log.i(TAG, "Paired device: " + device.getName() + ", " + device.getAddress() + ", " + device.getBluetoothClass());
            }
        }
    }

    @Override
    public void changeConnectionListener(ConnectionListener connectionListener) {

    }

    @Override
    public void close() {

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
