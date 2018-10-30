package peterson.ttu.edu.backupaids.network;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.io.IOException;

import peterson.ttu.edu.backupaids.util.ConnectionType;

public class BluetoothConnectionMaker implements ConnectionMaker {
    private final Context context;

    /* package */ BluetoothConnectionMaker(@NonNull Context context, ConnectionListener connectionListener, ConnectionType connectionType) throws IOException {
        this.context = context;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            throw new IOException("This device doesn't support Bluetooth; sending/receiving data will not work.");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            throw new BluetoothNotEnabledException("Bluetooth is not enabled; kindly ask the user to enable Bluetooth.");
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
