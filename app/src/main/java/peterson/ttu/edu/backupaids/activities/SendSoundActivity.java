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
import java.net.Socket;
import java.util.List;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.SoundPassthrough;

public class SendSoundActivity extends AppCompatActivity implements ConnectionManager.ConnectionListener {

    private boolean sending = false;
    private BluetoothMonitor bluetoothMonitor;
    private ConnectionManager connectionManager;
    private SoundPassthrough soundPassthrough;

    private boolean fullyStopped = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sound);

        // Listen for Bluetooth connections (for recording)
        bluetoothMonitor = BluetoothMonitor.createIfNeeded(this);
        IntentFilter connectFilter = new IntentFilter();
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        connectFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(bluetoothMonitor, connectFilter);

        soundPassthrough = new SoundPassthrough(this);

        connectionManager = new ConnectionManager(this, this);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bluetoothMonitor);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if ( Util.isScreenOn(this)) {
            // App closed or else we've switched activities. Stop what we're doing
            stopPlaying();
            unregisterReceiver(connectionManager);
            connectionManager.stopServiceDiscovery();
            connectionManager.unpublishService();
            fullyStopped = true;
        } else {
            // Screen turned off; we should keep going
            fullyStopped = false;
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( fullyStopped) {
            registerReceiver(connectionManager, Util.WIFI_P2P_INTENT_FILTER);
            connectionManager.publishService();
            connectionManager.beginServiceDiscovery();
        }
        fullyStopped = false;
    }

    public void sendSound(View view) {
        ImageButton playButton = findViewById(R.id.sendSoundStartButton);
        if ( soundPassthrough.isPlaying()) {
            stopPlaying();
        } else {
            // Start playing!
            Spinner sendSoundPeerSpinner = findViewById(R.id.sendSoundPeerSpinner);
            if ( sendSoundPeerSpinner == null ||
                 sendSoundPeerSpinner.getAdapter() == null ||
                 sendSoundPeerSpinner.getAdapter().getCount() == 0) {
                Toast.makeText(this, "No targets found", Toast.LENGTH_SHORT).show();
                return;
            }

            connectionManager.connect((String)sendSoundPeerSpinner.getSelectedItem());
            playButton.setImageResource(R.drawable.power_button_green2);
        }
    }

    private void stopPlaying() {
        soundPassthrough.stop();
        ImageButton playButton = findViewById(R.id.sendSoundStartButton);
        playButton.setImageResource(R.drawable.power_button_blue2);
    }

    @Override
    public void servicePublishingFailed() {
        Toast.makeText(this, "Unable to make ourselves visible to other phones.", Toast.LENGTH_LONG);
        finish();
    }

    public void supportedPeersChanged(List<String> supportedPeers) {
        updateSpinner(supportedPeers);
    }

    @Override
    public void connectionReady(Socket socket) throws IOException {
        sending = true;
        soundPassthrough.stream(socket);
    }

    @Override
    public void incomingConnection(Socket socket) throws IOException {
        // Shouldn't happen. Stop it
        throw new IOException("Shouldn't be a remote connection to this activity...");
    }

    @Override
    public void connectionFailed(final IOException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SendSoundActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG);
                stopPlaying();
            }
        });
    }

    @Override
    public void connectionClosed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopPlaying();
            }
        });
    }

    @Override
    public void servicesStarted() {
        // Nothing for us to do
    }

    @Override
    public void servicesStopped() {
        // Nothing for us to do
    }

    private void updateSpinner(List<String> supportedPeers) {
        Spinner sendSoundPeerSpinner = findViewById(R.id.sendSoundPeerSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, supportedPeers);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sendSoundPeerSpinner.setAdapter(arrayAdapter);
    }

}
