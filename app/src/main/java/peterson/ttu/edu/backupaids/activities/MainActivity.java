package peterson.ttu.edu.backupaids.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.activities.headsetSetup.PresetSetupActivity;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.SoundPassthrough;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private SoundPassthrough soundPassthrough;
    private ConnectionManager connectionManager;
    private Timer discoverableCountdown = null;
    private boolean fullyStopped = true;
    private Runnable todoOnServiceStopped;
    private boolean isDiscoverable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request audio recording permission. The app is useless without it, so ask up-front
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        connectionManager = new ConnectionManager(this, new ConnectionManager.ConnectionListener() {
            @Override
            public void supportedPeersChanged(List<String> supportedPeers) { }

            @Override
            public void connectionReady(Socket socket) throws IOException { }

            @Override
            public void incomingConnection(Socket socket) throws IOException {
                // We don't need to stop discovery (and, by extension, change the button back to gray)
                // since we made a connection
                if ( discoverableCountdown != null) {
                    discoverableCountdown.cancel();
                    discoverableCountdown = null;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageButton makeDiscoverableButton = findViewById(R.id.makeDiscoverable);
                        makeDiscoverableButton.setImageResource(R.drawable.phone_in_green);
                    }
                });
                soundPassthrough.playRemoteConnection(socket, getCurrentSoundPreset());
            }

            @Override
            public void connectionFailed(IOException e) {
                connectionManager.stopListeningForConnections();
                connectionManager.stopServiceDiscovery();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopServiceDiscovery(true);
                    }
                });
            }

            // We don't do this, so it won't be called
            @Override
            public void servicePublishingFailed() { }

            @Override
            public void connectionClosed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopServiceDiscovery(true);
                    }
                });
            }

            @Override
            public void servicesStarted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageButton makeDiscoverableButton = findViewById(R.id.makeDiscoverable);
                        makeDiscoverableButton.setImageResource(R.drawable.phone_in_blue);
                        isDiscoverable = true;
                    }
                });
            }

            @Override
            public void servicesStopped() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageButton makeDiscoverableButton = findViewById(R.id.makeDiscoverable);
                        makeDiscoverableButton.setImageResource(R.drawable.phone_in_gray);
                        isDiscoverable = false;
                        if ( todoOnServiceStopped != null) {
                            todoOnServiceStopped.run();
                            todoOnServiceStopped = null;
                        }
                    }
                });
            }
        });

        setContentView(R.layout.activity_main);

        updateSpinner();

        Spinner presetSpinner = findViewById(R.id.presetSpinner);
        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if ( soundPassthrough == null) {
                    // Still setting up
                    return;
                }
                if ( soundPassthrough.isPlaying()) {
                    // TODO: I guess we should re-connect in this case?
                    // Restart playback so we use the new preset
                    stopPlaying();
                    playSound(view);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Shouldn't happen
            }
        });
    }

    @Override
    protected void onDestroy() {
        stopServiceDiscovery(true);
        super.onDestroy();
    }

    private void stopActions() {
        stopPlaying();

        ImageButton toggleButton = (ImageButton) findViewById(R.id.makeDiscoverable);
        toggleButton.setImageResource(R.drawable.phone_in_gray);
        stopServiceDiscovery(true);
        connectionManager.unpublishService();
        unregisterReceiver(connectionManager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if ( fullyStopped) {
            registerReceiver(connectionManager, Util.WIFI_P2P_INTENT_FILTER);
        }
        fullyStopped = false;
    }

    private void updateSpinner() {
        SoundPresetManager presetManager = SoundPresetManager.getInstance(this);
        String[] presetNames = presetManager.getSortedPresetNames();
        Spinner presetSpinner = findViewById(R.id.presetSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, presetNames);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        presetSpinner.setAdapter(arrayAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissionToRecordAccepted = false;
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) {
            finish();
            return;
        }

        // Getting here means permission is granted!
        setUpAudioRecordingAndPlayback();
    }


    private void setUpAudioRecordingAndPlayback()
    {
        soundPassthrough = new SoundPassthrough(this);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    public void playSound(View view) {
        ImageButton playButton = findViewById(R.id.playSound);
        if ( soundPassthrough.isPlaying()) {
            stopPlaying();
        } else {
            // Start playing!
            try
            {
                SoundPreset preset = getCurrentSoundPreset();
                soundPassthrough.start(preset);
                playButton.setImageResource(R.drawable.power_button_green2);
            }
            catch(IOException e)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Error initializing app");
                builder.setMessage("Unable to set up audio recording/playback.");
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

    public void makeDiscoverable(View view) {
        if ( isDiscoverable) {
            stopServiceDiscovery(true);
            // Stop everything
        } else {
            connectionManager.listenForConnections();
            connectionManager.publishService();
            connectionManager.beginServiceDiscovery();

            // Set a timer so we aren't discoverable forever (which wouldn't be allowed anyway)
            discoverableCountdown = new Timer();
            discoverableCountdown.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopServiceDiscovery(false);
                        }
                    });
                }
            }, 30000); // 30 seconds
        }
    }

    private void stopServiceDiscovery(boolean stopConnectionsAlso) {
        if ( discoverableCountdown != null) {
            discoverableCountdown.cancel();
            discoverableCountdown = null;
        }
        connectionManager.stopServiceDiscovery();
        connectionManager.unpublishService();
        if ( stopConnectionsAlso) {
            connectionManager.stopListeningForConnections();
        }
    }

    private SoundPreset getCurrentSoundPreset() {
        Spinner presetSpinner = findViewById(R.id.presetSpinner);
        int position = presetSpinner.getSelectedItemPosition();
        if ( position >= 0) {
            return SoundPresetManager.getInstance(this).getPreset(position);
        }
        return null;
    }

    private void stopPlaying() {
        if ( soundPassthrough == null) {
            // Not initialized yet
            return;
        }
        soundPassthrough.stop();
        ImageButton playButton = findViewById(R.id.playSound);
        playButton.setImageResource(R.drawable.power_button_blue2);
    }

    public void newPreset(View view) {
        startActivityForResult(new Intent(this, PresetSetupActivity.class), 0);
    }

    public void editPreset(View view) {
        Intent intent = new Intent(this, PresetSetupActivity.class);
        Spinner presetSpinner = findViewById(R.id.presetSpinner);
        intent.putExtra(Util.SELECTED_PRESET_ITEM, presetSpinner.getSelectedItemPosition());
        startActivity(intent);
    }

    public void sendToOtherPhone(View view) {
        // Stop all this stuff now so that SendSoundActivity can start service discovery fresh
        // (otherwise I think there's a race condition between us stopping and the other starting)
        // Use the runnable to kick off the activity only when the service stuff is stopped
        todoOnServiceStopped = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(MainActivity.this, SendSoundActivity.class));
            }
        };
        stopActions();
        fullyStopped = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateSpinner();
    }
}
