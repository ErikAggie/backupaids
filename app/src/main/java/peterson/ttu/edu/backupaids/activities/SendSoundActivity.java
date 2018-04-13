package peterson.ttu.edu.backupaids.activities;

import android.bluetooth.BluetoothDevice;
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
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.network.ConnectionListener;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.BaseSound;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

public class SendSoundActivity extends AppCompatActivity implements ConnectionListener {

    private BluetoothMonitor bluetoothMonitor;
    private ConnectionManager connectionManager;
    private BaseSound streamRecording;
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

        connectionManager = new ConnectionManager(this, this);
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

            connectionManager.makeConnection((String)sendSoundPeerSpinner.getSelectedItem());
        }
    }

    private void stopPlaying() {
        if ( streamRecording != null) {
            streamRecording.stop();
            streamRecording = null;
            ImageButton playButton = findViewById(R.id.sendSoundStartButton);
            playButton.setImageResource(R.drawable.power_button_blue2);
        }
    }

    @Override
    public void servicePublishingFailed() {
        Toast.makeText(this, "Unable to make ourselves visible to other phones.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void foundAPeer(String newPeer) {
        peers.add(newPeer);
        updateSpinner();
    }

    @Override
    public void connectionReady(Socket socket) throws IOException {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton playButton = findViewById(R.id.sendSoundStartButton);
                playButton.setImageResource(R.drawable.power_button_green2);
            }
        });
        streamRecording = new BaseSound(SourceFactory.createMicAudioRecord(), DestinationFactory.createRemoteSoundDestination(socket));
        streamRecording.playAudio();
    }

    @Override
    public void connectionFailed(final IOException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopPlaying();
                Toast.makeText(SendSoundActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                ImageButton playButton = findViewById(R.id.sendSoundStartButton);
                playButton.setImageResource(R.drawable.power_button_blue2);
            }
        });
    }

    @Override
    public void connectionClosed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopPlaying();
                ImageButton playButton = findViewById(R.id.sendSoundStartButton);
                playButton.setImageResource(R.drawable.power_button_blue2);
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

    private void updateSpinner() {
        Spinner sendSoundPeerSpinner = findViewById(R.id.sendSoundPeerSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, peers);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sendSoundPeerSpinner.setAdapter(arrayAdapter);
    }

}
