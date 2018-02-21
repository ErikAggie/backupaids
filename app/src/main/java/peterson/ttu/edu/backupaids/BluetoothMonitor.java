package peterson.ttu.edu.backupaids;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

/**
 * Created by erika on 2/21/2018.
 */

public class BluetoothMonitor extends BroadcastReceiver {

    private static final String TAG = "BluetoothMonitor";
    private static BluetoothMonitor smInstance;
    private final Activity mActivity;
    private BluetoothDevice mConnectedDevice;

    private boolean headsetConnected;


    /**
     * This is public so the system can create it. It should not be called directly!
     */
    private BluetoothMonitor(Activity activity) {
        mActivity = activity;
        Log.e(TAG, "Hello!");

        // TODO: detect what's connected at startup
        /*        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                // TODO: record both if profile is headset...and the user wants it
                Log.i(TAG, "Bluetooth connected: " + i);
                List<BluetoothDevice> bluetoothDeviceList = bluetoothProfile.getConnectedDevices();
                for ( BluetoothDevice bluetoothDevice : bluetoothDeviceList) {
                    Log.i(TAG, "Bluetooth device: " + bluetoothDevice.getName() + " connected!");
                }
            }

            @Override
            public void onServiceDisconnected(int i) {
                // TODO: if this was our profile, stop recording with bluetooth mixed in
                Log.i(TAG, "Bluetooth disconnected: " + i);
            }
        };
        if ( !bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HEADSET)) {
            Log.e(TAG, "Unable to get bluetooth profile.");
        }*/


//        bluetoothAdapter.getProfileConnectionState(BluetoothMonitor.)
//
//        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_CALL,
//                Util.SAMPLE_RATE,
//                AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                bufferSize);

    }

    public static BluetoothMonitor createInstance(Activity activity) {
        if ( smInstance != null) {
            throw new RuntimeException("Can only be one BluetoothMonitor!");
        }
        synchronized (BluetoothMonitor.class) {
            if ( smInstance != null) {
                throw new RuntimeException("Can only be one BluetoothMonitor!");
            }
            smInstance = new BluetoothMonitor(activity);
        }
        return smInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
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

    public boolean isHeadsetConnected() {
        return mConnectedDevice != null;
    }
}
