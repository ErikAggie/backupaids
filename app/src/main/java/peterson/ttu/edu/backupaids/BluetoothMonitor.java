package peterson.ttu.edu.backupaids;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

/**
 * Monitors for Bluetooth connections (for recording from a Bluetooth source)
 */

public class BluetoothMonitor extends BroadcastReceiver {

    private static final String TAG = "BluetoothMonitor";
    private static BluetoothMonitor smInstance;
    private BluetoothDevice mConnectedDevice;

    /**
     * This is public so the system can create it. It should not be called directly!
     */
    private BluetoothMonitor(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                if ( bluetoothProfile.getConnectedDevices().size() > 0) {
                    // TODO: handle other bluetooth devices (fitness trackers, etc.)
                    mConnectedDevice = bluetoothProfile.getConnectedDevices().get(0);
                }
            }

            @Override
            public void onServiceDisconnected(int i) {
                // Don't care; this is handled below
            }
        };
        if ( !bluetoothAdapter.getProfileProxy(context.getApplicationContext(), serviceListener, BluetoothProfile.HEADSET)) {
            Log.e(TAG, "Unable to get bluetooth profile.");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ( action == null) {
            return;
        }
        switch (action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                mConnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                mConnectedDevice = null;
                break;
            case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED:
                break;
        }
        Log.d(TAG, "Received " + action);
    }

    public static BluetoothMonitor getInstance() {
        return smInstance;
    }

    public static BluetoothMonitor createIfNeeded(Context context) {
        if ( smInstance == null) {
            synchronized (BluetoothMonitor.class) {
                if (smInstance == null) {
                    smInstance = new BluetoothMonitor(context);
                }
            }
        }
        return smInstance;

    }

    public boolean isHeadsetConnected() {
        return mConnectedDevice != null;
    }
}
