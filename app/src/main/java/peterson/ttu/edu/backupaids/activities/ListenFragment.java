package peterson.ttu.edu.backupaids.activities;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.controller.ConnectionController;
import peterson.ttu.edu.backupaids.controller.ListenConnectionController;
import peterson.ttu.edu.backupaids.controller.PeerCallback;
import peterson.ttu.edu.backupaids.activities.headsetSetup.PresetSetupActivity;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.service.LocalSoundService;
import peterson.ttu.edu.backupaids.service.RemoteSoundService;

public class ListenFragment extends Fragment implements View.OnClickListener, ConnectionController.Listener {

    private Runnable todoOnServiceStopped;

    // For showing a popup with available connections
    private ConnectionPopupFragment connectionPopup;

    private ConnectionController connectionController;

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

    //----------------------------------------------------------------------------------------------
    // Lifecycle methods
    //----------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View fragmentView = inflater.inflate(R.layout.fragment_listen, container, false);


        // Set up our button's onClickEvents (why can't they target this automatically???
        Button newPresetButton = fragmentView.findViewById(R.id.newPresetButton);
        newPresetButton.setOnClickListener(this);
        ImageButton playButton = fragmentView.findViewById(R.id.playSound);
        playButton.setOnClickListener(this);
        ImageButton makeDiscoverableButton = fragmentView.findViewById(R.id.makeDiscoverable);
        makeDiscoverableButton.setOnClickListener(this);

        return fragmentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView ourPin = getView().findViewById(R.id.ourPinTextView);
        ourPin.setText(getString(R.string.our_pin, ConnectionMaker.getPin()));

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

        if ( RemoteSoundService.isCurrentlyStreaming()) {
            // Already streaming. Need to re-connect with this guy
            connectionController = new ListenConnectionController(getContext(), getActivity(), this);
        }

        // We could already be playing, so check on that...
        updatePlayButton();
        updateMakeDiscoverableButton();
    }


    @Override
    public void onDestroyView() {
        stopStreaming();
        stopPlaying();
        super.onDestroyView();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if ( isVisibleToUser) {
            LocalSoundService.registerListener(listener);
        } else {
            LocalSoundService.unregisterListener(listener);
        }
    }

    //----------------------------------------------------------------------------------------------
    // UI action methods
    //----------------------------------------------------------------------------------------------

    @Override
    public void onClick(View view) {
        switch ( view.getId()) {
            case R.id.playSound:
                playLocalSound();
                break;
            case R.id.newPresetButton:
                newPreset();
                break;
            case R.id.makeDiscoverable:
                makeDiscoverable();
                break;
            default:
                throw new RuntimeException("Unexpected button push!");
        }
    }

    //----------------------------------------------------------------------------------------------
    // Helper methods
    //----------------------------------------------------------------------------------------------

    private void playLocalSound() {
        if ( LocalSoundService.isLocalSoundServiceRunning()) {
            stopPlaying();
        } else {
            // Start playing!
            getActivity().startService(new Intent(getContext(), LocalSoundService.class));
        }
    }

    private void newPreset() {
        startActivityForResult(new Intent(getContext(), PresetSetupActivity.class), 0);
    }

    private void makeDiscoverable() {
        if ( connectionController != null) {
            stopStreaming();
        } else {
            connectionController = new ListenConnectionController(getContext(), getActivity(), this);
            Toast.makeText(getContext(), "Looking for other devices...this will take a few seconds.", Toast.LENGTH_LONG).show();
        }
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
        if ( LocalSoundService.isLocalSoundServiceRunning()) {
            playButton.setImageResource(R.drawable.ic_power_button_green);
        } else {
            playButton.setImageResource(R.drawable.ic_power_button_blue);
        }
    }

    private void updateMakeDiscoverableButton() {
        ImageButton makeDiscoverableButton = getView().findViewById(R.id.makeDiscoverable);
        if ( connectionController == null) {
            makeDiscoverableButton.setImageResource(R.drawable.phone_in_gray);
            return;
        }

        switch (connectionController.getState()) {
            case STARTUP:
            case CONNECTING:
                makeDiscoverableButton.setImageResource(R.drawable.phone_in_blue);
                break;
            case STREAMING:
                makeDiscoverableButton.setImageResource(R.drawable.phone_in_green);
                break;
            case FAILED:
            case STOPPED:
                makeDiscoverableButton.setImageResource(R.drawable.phone_in_gray);
                break;
            default:
                throw new RuntimeException("Unknown connection state " + connectionController.getState() + "!");
        }
    }

    private SoundPreset getCurrentSoundPreset() {
        Spinner presetSpinner = getView().findViewById(R.id.presetSpinner);
        int position = presetSpinner.getSelectedItemPosition();
        if ( position >= 0) {
            return SoundPresetManager.getInstance(getContext()).getPreset(position);
        }
        return null;
    }

    private void stopStreaming() {
        if ( connectionController != null) {
            connectionController.stop();
            connectionController = null;
        }
    }

    private void stopPlaying() {
        if ( LocalSoundService.isLocalSoundServiceRunning()) {
            getActivity().stopService(new Intent(getContext(), LocalSoundService.class));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateSpinner();
    }

    //---------------------------------------------------------------------------------------------
    // ConnectionController callbacks
    //---------------------------------------------------------------------------------------------

    @Override
    public void stateChanged(final ConnectionController.State state) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMakeDiscoverableButton();
                switch ( state) {
                    case STREAMING:
                        if ( connectionPopup != null) {
                            connectionPopup.dismiss();
                            connectionPopup = null;
                        }
                        break;
                    case FAILED:
                        Toast.makeText(getContext(), "Connection failed. Try again in a few seconds.", Toast.LENGTH_LONG).show();
                        // Don't have to stop since we'll get a STOPPED state shortly...
                        break;
                    case STOPPED:
                        Toast.makeText(getContext(), "Connection to the other device was closed.", Toast.LENGTH_SHORT).show();
                        if ( connectionPopup != null) {
                            connectionPopup.dismiss();
                            connectionPopup = null;
                        }
                        if ( todoOnServiceStopped != null) {
                            todoOnServiceStopped.run();
                            todoOnServiceStopped = null;
                        }
                        connectionController = null;
                        break;
                    default:
                        // Nothing to do...
                }
            }
        });
    }

    @Override
    public void askAboutConnection(final String connectionName, final PeerCallback callback) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( connectionPopup != null) {
                    connectionPopup.dismiss();
                }
                connectionPopup = new ConnectionPopupFragment();
                connectionPopup.setListener(
                        new ConnectionPopupFragment.OnFragmentInteractionListener() {
                            @Override
                            public void connectionConfirmed() {
                                callback.approveConnection(connectionName);
                            }

                            @Override
                            public void cancelled() {
                                callback.denyConnection(connectionName);
                                if (connectionPopup != null) {
                                    connectionPopup.dismiss();
                                    connectionPopup = null;
                                }
                            }
                        });
                connectionPopup.setTargetFragment(ListenFragment.this, 1);
                connectionPopup.setConnection(connectionName);
                connectionPopup.show(getFragmentManager(), "Connections");
            }
        });
    }
}