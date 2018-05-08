package peterson.ttu.edu.backupaids.activities;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
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

public class ListenFragment extends Fragment implements ConnectionPopupFragment.OnFragmentInteractionListener, ConnectionManager.PeerListener, BaseStreamService.Listener {


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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePlayButton();
                }
            });
        }

        @Override
        public void playbackStopped() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePlayButton();
                }
            });
        }
    };

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        setContentView(R.layout.activity_main);

        TextView ourPin = getView().findViewById(R.id.ourPinTextView);
        ourPin.setText(getString(R.string.our_pin, ConnectionManager.getPin()));

        updateSpinner();

        Spinner presetSpinner = getView().findViewById(R.id.presetSpinner);
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
    public void onDestroyView() {
        stopListening();
        stopPlaying();
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalSoundService.registerListener(listener);
    }

    @Override
    public void onPause() {
        LocalSoundService.unregisterListener(listener);
        super.onPause();
    }

    private void updateSpinner() {
        SoundPresetManager presetManager = SoundPresetManager.getInstance(getContext());
        String[] presetNames = presetManager.getSortedPresetNames();
        Spinner presetSpinner = getView().findViewById(R.id.presetSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item, presetNames);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        presetSpinner.setAdapter(arrayAdapter);
    }

    private void updatePlayButton() {
        ImageButton playButton = getView().findViewById(R.id.playSound);
        if ( LocalSoundService.isRunning()) {
            playButton.setImageResource(R.drawable.ic_power_button_green);
        } else {
            playButton.setImageResource(R.drawable.ic_power_button_blue);
        }
    }

    private void updateMakeDiscoverableButton() {
        ImageButton makeDiscoverableButton = getView().findViewById(R.id.makeDiscoverable);
        if ( RemoteSoundService.isCurrentlyStreaming()) {
            makeDiscoverableButton.setImageResource(R.drawable.phone_in_green);
        } else if ( connectionManager != null) {
            makeDiscoverableButton.setImageResource(R.drawable.phone_in_blue);
        } else {
            makeDiscoverableButton.setImageResource(R.drawable.phone_in_gray);
        }
    }

    public void playLocalSound(View view) throws IOException {
        if ( LocalSoundService.isRunning()) {
            stopPlaying();
        } else {
            // Start playing!
            getActivity().startService(new Intent(getContext(), LocalSoundService.class));
        }
    }

    public void makeDiscoverable(View view) {
        if ( connectionManager != null || RemoteSoundService.isCurrentlyStreaming()) {
            stopListening();
        } else {
            connectionManager = new ConnectionManager(getContext(), this);
            Toast.makeText(getContext(), "Looking for other devices...this will take a few seconds.", Toast.LENGTH_LONG).show();

            // Set a timer so we aren't discoverable forever (which wouldn't be allowed anyway)
            discoverableCountdown = new Timer();
            discoverableCountdown.schedule(new TimerTask() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopListening();
                            Toast.makeText(getContext(), "Looking for other devices timed out.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }, 180000); // 2 minutes
        }
    }

    private void stopListening() {
        getActivity().stopService(new Intent(getContext(), RemoteSoundService.class));
        getActivity().stopService(new Intent(getContext(), LocalSoundService.class));
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
        Spinner presetSpinner = getView().findViewById(R.id.presetSpinner);
        int position = presetSpinner.getSelectedItemPosition();
        if ( position >= 0) {
            return SoundPresetManager.getInstance(getContext()).getPreset(position);
        }
        return null;
    }

    private void stopPlaying() {
        if ( LocalSoundService.isRunning()) {
            getActivity().stopService(new Intent(getContext(), LocalSoundService.class));
        }
        if ( RemoteSoundService.isCurrentlyStreaming()) {
            getActivity().stopService(new Intent(getContext(), RemoteSoundService.class));
        }
        if ( connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
    }

    public void newPreset(View view) {
        startActivityForResult(new Intent(getContext(), PresetSetupActivity.class), 0);
    }

    public void editPreset(View view) {
        Intent intent = new Intent(getContext(), PresetSetupActivity.class);
        Spinner presetSpinner = getView().findViewById(R.id.presetSpinner);
        intent.putExtra(Util.SELECTED_PRESET_ITEM, presetSpinner.getSelectedItemPosition());
        startActivity(intent);
    }

    public void sendToOtherPhone(View view) {
        // Stop all this stuff now so that SpeakFragment can start service discovery fresh
        // (otherwise I think there's a race condition between us stopping and the other starting)
        // Use the runnable to kick off the activity only when the service stuff is stopped
        if ( connectionManager != null) {
            todoOnServiceStopped = new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(getContext(), SpeakFragment.class));
                }
            };
            stopListening();
        } else {
            startActivity(new Intent(getContext(), SpeakFragment.class));
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        updateSpinner();
//    }

    @Override
    public void servicesStarted() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMakeDiscoverableButton();
            }
        });
    }

    @Override
    public void servicesStopped() {
        getActivity().runOnUiThread(new Runnable() {
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
        getActivity().runOnUiThread(new Runnable() {
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
                connectionPopup.show(getActivity().getSupportFragmentManager(), "Connections");
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
        getActivity().startService(new Intent(getContext(), RemoteSoundService.class));
        // Service takes over this connection manager
        connectionManager = null;
    }

    @Override
    public void connectionConfirmed(String connectionName) {
        RemoteSoundService.registerListener(this);
        Intent intent = new Intent(getContext(), RemoteSoundService.class);
        intent.putExtra(Util.CONNECTION_NAME_EXTRA, connectionName);
        getActivity().startService(intent);

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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMakeDiscoverableButton();
            }
        });
    }

    @Override
    public void connectionFailed() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "Connection to the other device failed.", Toast.LENGTH_SHORT).show();
                updateMakeDiscoverableButton();
            }
        });
    }

    @Override
    public void connectionClosed() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "Connection to the other device was closed.", Toast.LENGTH_SHORT).show();
                updateMakeDiscoverableButton();
            }
        });
    }
}