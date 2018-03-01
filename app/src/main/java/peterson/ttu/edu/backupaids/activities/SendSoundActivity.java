package peterson.ttu.edu.backupaids.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.SoundPassthrough;

public class SendSoundActivity extends AppCompatActivity implements ConnectionManager.SupportedPeersChangeListener {

    private BluetoothMonitor bluetoothMonitor;
    private ConnectionManager connectionManager;
    private SoundPassthrough soundPassthrough;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sound);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        soundPassthrough = new SoundPassthrough(this);

        connectionManager = new ConnectionManager(this, this);

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
        ImageButton playButton = findViewById(R.id.sendSoundStartButton);
        if ( soundPassthrough.isPlaying()) {
            stopPlaying();
        } else {
            // Start playing!
            Spinner sendSoundPeerSpinner = findViewById(R.id.sendSoundPeerSpinner);
            if ( sendSoundPeerSpinner.getAdapter().getCount() == 0) {
                Toast.makeText(this, "No peers found", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                connectionManager.connect((String)sendSoundPeerSpinner.getSelectedItem());
                // TODO: this will be in a callback...
                soundPassthrough.start(null);
                playButton.setImageResource(R.drawable.power_button_green2);
            }
            catch(IOException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Error initializing app");
                builder.setMessage("Unable to set up audio recording/sending.");
                builder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                builder.create().show();
            }

        }
    }

    private void stopPlaying() {
        soundPassthrough.stop();
        ImageButton playButton = findViewById(R.id.sendSoundStartButton);
        playButton.setImageResource(R.drawable.power_button_blue2);
    }

    public void supportedPeersChanged(List<String> supportedPeers) {
        updateSpinner(supportedPeers);
    }

    private void updateSpinner(List<String> supportedPeers) {
        Spinner sendSoundPeerSpinner = findViewById(R.id.sendSoundPeerSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, supportedPeers);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sendSoundPeerSpinner.setAdapter(arrayAdapter);
    }

}
