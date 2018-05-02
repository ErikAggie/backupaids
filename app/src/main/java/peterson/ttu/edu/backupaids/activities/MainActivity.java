package peterson.ttu.edu.backupaids.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.activities.headsetSetup.PresetSetupActivity;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.service.BaseStreamService;
import peterson.ttu.edu.backupaids.service.LocalSoundService;
import peterson.ttu.edu.backupaids.service.RemoteSoundService;
import peterson.ttu.edu.backupaids.service.StreamSoundService;

public class MainActivity extends AppCompatActivity implements ConnectionPopupFragment.OnFragmentInteractionListener, ConnectionManager.PeerListener, BaseStreamService.Listener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String PLAY_LOCAL_SERVICE_STRING = "PlayLocalRecording";

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private ConnectionManager connectionManager;
    private Timer discoverableCountdown = null;
    private Runnable todoOnServiceStopped;

    // For showing a popup with available connections
    private ConnectionPopupFragment connectionPopup;

    /**
     * Listener for local sound events
     */
    private final LocalSoundService.Listener listener = new LocalSoundService.Listener() {
        @Override
        public void playbackStarted() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePlayButton();
                }
            });
        }

        @Override
        public void playbackStopped() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePlayButton();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request audio recording permission. The app is useless without it, so ask up-front
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        setContentView(R.layout.activity_main);

        TextView ourPin = findViewById(R.id.ourPinTextView);
        ourPin.setText(getString(R.string.our_pin, ConnectionManager.getPin()));

        updateSpinner();

        Spinner presetSpinner = findViewById(R.id.presetSpinner);
        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SoundPreset newPreset = getCurrentSoundPreset();
                if ( newPreset != null) {
                    LocalSoundService.setCurrentPreset(newPreset.getName());
                    RemoteSoundService.setCurrentPreset(newPreset.getName());
                }
                // TODO: we're making the user restart playback; it'd be nice if we could do
                // it for them, but the problem is this gets called as the Activity is being
                // created, so cases where we're arriving while we're already playing
                // (e.g. if a service kicks us off) then stopping playback would be bad
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Shouldn't happen
            }
        });

        SoundPreset initialPreset = getCurrentSoundPreset();
        if ( initialPreset != null) {
            LocalSoundService.setCurrentPreset(initialPreset.getName());
            RemoteSoundService.setCurrentPreset(initialPreset.getName());
        }

        // We could already be playing, so check on that...
        updatePlayButton();
        updateMakeDiscoverableButton();
    }

    @Override
    protected void onDestroy() {
        stopListening();
        stopPlaying();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalSoundService.registerListener(listener);
    }

    @Override
    protected void onStop() {
        LocalSoundService.unregisterListener(listener);
        super.onStop();
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

    private void updatePlayButton() {
        ImageButton playButton = findViewById(R.id.playSound);
        if ( LocalSoundService.isRunning()) {
            playButton.setImageResource(R.drawable.power_button_green2);
        } else {
            playButton.setImageResource(R.drawable.power_button_blue2);
        }
    }

    private void updateMakeDiscoverableButton() {
        ImageButton makeDiscoverableButton = findViewById(R.id.makeDiscoverable);
        if ( RemoteSoundService.isCurrentlyStreaming()) {
            makeDiscoverableButton.setImageResource(R.drawable.phone_in_green);
        } else if ( connectionManager != null) {
            makeDiscoverableButton.setImageResource(R.drawable.phone_in_blue);
        } else {
            makeDiscoverableButton.setImageResource(R.drawable.phone_in_gray);
        }
    }

    private void setUpAudioRecordingAndPlayback()
    {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    public void playLocalSound(View view) throws IOException {
        if ( LocalSoundService.isRunning()) {
            stopPlaying();
        } else {
            // Start playing!
            startService(new Intent(this, LocalSoundService.class));
        }
    }

    public void makeDiscoverable(View view) {
        if ( connectionManager != null || RemoteSoundService.isCurrentlyStreaming()) {
            stopListening();
        } else {
            connectionManager = new ConnectionManager(this);
            connectionManager.setPeerListener(this);

            // Set a timer so we aren't discoverable forever (which wouldn't be allowed anyway)
            discoverableCountdown = new Timer();
            discoverableCountdown.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopListening();
                        }
                    });
                }
            }, 180000); // 2 minutes
        }
    }

    private void stopListening() {
        stopService(new Intent(this, RemoteSoundService.class));
        stopService(new Intent(this, LocalSoundService.class));
        if ( discoverableCountdown != null) {
            discoverableCountdown.cancel();
            discoverableCountdown = null;
        }
        if ( connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
        updateMakeDiscoverableButton();
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
        if ( LocalSoundService.isRunning()) {
            stopService(new Intent(this, LocalSoundService.class));
        }
        if ( RemoteSoundService.isCurrentlyStreaming()) {
            stopService(new Intent(this, RemoteSoundService.class));
        }
        if ( connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
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
        if ( connectionManager != null) {
            todoOnServiceStopped = new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(MainActivity.this, SendSoundActivity.class));
                }
            };
            stopListening();
        } else {
            startActivity(new Intent(MainActivity.this, SendSoundActivity.class));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateSpinner();
    }

    @Override
    public void servicesStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMakeDiscoverableButton();
            }
        });
    }

    @Override
    public void servicesStopped() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( connectionPopup != null) {
                    connectionPopup.dismiss();
                    connectionPopup = null;
                }
                connectionManager = null;
                updateMakeDiscoverableButton();
                if ( todoOnServiceStopped != null) {
                    todoOnServiceStopped.run();
                    todoOnServiceStopped = null;
                }
            }
        });
    }

    @Override
    public void servicePublishingFailed() {
        // TODO: should do something here...
        connectionManager = null;
        updateMakeDiscoverableButton();
    }

    @Override
    public void foundAPeer(final String peerName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( discoverableCountdown != null) {
                    discoverableCountdown.cancel();
                    discoverableCountdown = null;
                }
                if ( connectionPopup != null) {
                    connectionPopup.dismiss();
                }
                connectionPopup = new ConnectionPopupFragment();
                connectionPopup.setConnection(peerName);
                connectionPopup.show(getSupportFragmentManager(), "Connections");
            }
        });
    }

    @Override
    public void findingPeerFailed(IOException e) {
        // TODO: do something here...
    }

    @Override
    public void connectionWaiting() {
        if ( connectionPopup != null) {
            connectionPopup.dismiss();
            connectionPopup = null;
        }

        RemoteSoundService.registerListener(this);
        connectionManager.removePeerListener(this);
        startService(new Intent(this, RemoteSoundService.class));
        // Service takes over this connection manager
        connectionManager = null;
    }

    @Override
    public void connectionConfirmed(String connectionName) {
        RemoteSoundService.registerListener(this);
        Intent intent = new Intent(this, RemoteSoundService.class);
        intent.putExtra(Util.CONNECTION_NAME_EXTRA, connectionName);
        startService(intent);

        // Service takes over this connection manager
        connectionManager.removePeerListener(this);
        connectionManager = null;
    }

    @Override
    public void cancelled() {
        if ( connectionPopup != null) {
            connectionPopup.dismiss();
            connectionPopup = null;
        }
        stopListening();
    }

    @Override
    public void connectionMade() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMakeDiscoverableButton();
            }
        });
    }

    @Override
    public void connectionFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMakeDiscoverableButton();
            }
        });
    }

    @Override
    public void connectionClosed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMakeDiscoverableButton();
            }
        });
    }
}