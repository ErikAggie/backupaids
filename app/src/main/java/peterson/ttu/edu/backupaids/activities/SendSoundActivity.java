package peterson.ttu.edu.backupaids.activities;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.service.LocalSoundService;
import peterson.ttu.edu.backupaids.service.StreamSoundService;

public class SendSoundActivity extends AppCompatActivity implements ConnectionManager.PeerListener, StreamSoundService.Listener {

    private BluetoothMonitor bluetoothMonitor;
    private ConnectionManager connectionManager;
    private LocalSoundService streamRecording;
    private final List<String> peers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sound);

        TextView ourPin = findViewById(R.id.sendSoundOurPinTextView);
        ourPin.setText(getString(R.string.our_pin, ConnectionManager.getPin()));

        // Listen for Bluetooth connections (for recording)
        IntentFilter connectFilter = new IntentFilter();
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        connectFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(bluetoothMonitor, connectFilter);

        // If we're already streaming (i.e. we've been woken up), find the existing connection manager
        if ( StreamSoundService.isCurrentlyStreaming()) {
            connectionManager = ConnectionManager.getInstance();
        } else {
            connectionManager = new ConnectionManager(this, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectionManager.setPeerListener(this);
        StreamSoundService.registerListener(this);
    }

    @Override
    protected void onStop() {
        connectionManager.removePeerListener(this);
        StreamSoundService.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if ( connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
        super.onDestroy();
    }

    public void sendSound(View view) {
        if ( streamRecording != null) {
            stopService(new Intent(this, LocalSoundService.class));
        } else {
            // Start playing!
            Spinner sendSoundPeerSpinner = findViewById(R.id.sendSoundPeerSpinner);
            if ( sendSoundPeerSpinner == null ||
                 sendSoundPeerSpinner.getAdapter() == null ||
                 sendSoundPeerSpinner.getAdapter().getCount() == 0) {
                Toast.makeText(this, "No peers found", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, LocalSoundService.class);
            intent.putExtra(Util.CONNECTION_NAME_EXTRA, ((String)sendSoundPeerSpinner.getSelectedItem()));
            startService(intent);

            connectionManager.makeConnection((String)sendSoundPeerSpinner.getSelectedItem());
        }
    }

    private void updatePlayButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton playButton = findViewById(R.id.sendSoundStartButton);
                if (StreamSoundService.isCurrentlyStreaming()) {
                    playButton.setImageResource(R.drawable.power_button_green2);
                } else {
                    playButton.setImageResource(R.drawable.power_button_blue2);
                }
            }
        });
    }

    @Override
    public void servicePublishingFailed() {
        Toast.makeText(this, "Unable to make ourselves visible to other phones.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void foundAPeer(String newPeer) {
        // TODO: probably better as a notification a la MainActivity
        peers.add(newPeer);
        updateSpinner();
    }

    @Override
    public void findingPeerFailed(IOException e) {
        // TODO: Show something...
    }

    @Override
    public void servicesStarted() {
        // Nothing for us to do
    }

    @Override
    public void servicesStopped() {
        // Nothing for us to do
    }

    private void updateSpinner() {
        Spinner sendSoundPeerSpinner = findViewById(R.id.sendSoundPeerSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, peers);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sendSoundPeerSpinner.setAdapter(arrayAdapter);
    }

    @Override
    public void connectionMade() {
        updatePlayButton();
    }

    @Override
    public void connectionFailed() {
        // TODO: should say something here...
        updatePlayButton();
    }

    @Override
    public void connectionClosed() {
        // TODO: should say something here...
        updatePlayButton();
    }
}
