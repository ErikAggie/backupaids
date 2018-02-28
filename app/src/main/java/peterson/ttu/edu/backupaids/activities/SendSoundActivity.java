package peterson.ttu.edu.backupaids.activities;

import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.network.ConnectionManager;

public class SendSoundActivity extends AppCompatActivity {

    private BluetoothMonitor bluetoothMonitor;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sound);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectionManager = new ConnectionManager(this);

        // Listen for Bluetooth connections (for recording)
        bluetoothMonitor = BluetoothMonitor.createIfNeeded(this);
        IntentFilter connectFilter = new IntentFilter();
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        connectFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(bluetoothMonitor, connectFilter);
    }

    @Override
    protected void onPause() {
        connectionManager.stopServiceDiscovery();
        unregisterReceiver(connectionManager);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(connectionManager, Util.WIFI_P2P_INTENT_FILTER);
        connectionManager.beginServiceDiscovery();
    }

    public void sendSound(View view) {
        // TODO: fill in...
    }
}
