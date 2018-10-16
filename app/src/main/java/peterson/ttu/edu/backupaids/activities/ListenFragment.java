package peterson.ttu.edu.backupaids.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
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
import peterson.ttu.edu.backupaids.util.Util;

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

        @Override
        public void playbackErrored(final String error) {
            showPlaybackError(error);
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
        // TODO: update with the new preset button
        //Button newPresetButton = fragmentView.findViewById(R.id.newPresetButton);
        //newPresetButton.setOnClickListener(this);
        ImageButton playButton = fragmentView.findViewById(R.id.playSound);
        playButton.setOnClickListener(this);
        ImageButton makeDiscoverableButton = fragmentView.findViewById(R.id.makeDiscoverable);
        makeDiscoverableButton.setOnClickListener(this);
        ImageButton presetChooserButton = fragmentView.findViewById(R.id.presetChooser);
        presetChooserButton.setOnClickListener(this);

        return fragmentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView ourPin = getView().findViewById(R.id.ourPinTextView);
        ourPin.setText(getString(R.string.our_pin, ConnectionMaker.getPin()));

        updateSpinner();

        // TODO: shouldn't this be handled by the preset manager (i.e. services can listen when needed)?
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
            case R.id.presetChooser:
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
        AudioManager audioManager = getActivity().getApplicationContext().getSystemService(AudioManager.class);

        if ( connectionController != null) {
            stopStreaming();
        } else {
            if ( !Util.areHeadphonesActive(audioManager)) {
                // No headphones=no reason to try to stream (would just be annoying if we waited
                // until we connected to notice this...)
                showPlaybackError(getString(R.string.headphones_not_connected));
                return;
            }
            connectionController = new ListenConnectionController(getContext(), getActivity(), this);
            Toast.makeText(getContext(), "Looking for other devices...this will take a few seconds.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateSpinner() {
        // TODO: update text with selected preset, if needed
        // Use code here when popping up the list of presets
//        SoundPresetManager presetManager = SoundPresetManager.getInstance(getContext());
//        String[] presetNames = presetManager.getSortedPresetNames();
//        Spinner presetSpinner = getView().findViewById(R.id.presetSpinner);
//        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item, presetNames);
//        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
//        presetSpinner.setAdapter(arrayAdapter);
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
        TextView textForMakeDiscoverableButton = getView().findViewById(R.id.textForMakeDiscoverableButton);
        TextView pinTextView = getView().findViewById(R.id.ourPinTextView);
        if ( connectionController == null) {
            makeDiscoverableButton.setImageResource(R.drawable.ic_phone_in_gray);
            textForMakeDiscoverableButton.setText(R.string.not_connected);
            return;
        }

        switch (connectionController.getState()) {
            case STARTUP:
            case CONNECTING:
                makeDiscoverableButton.setImageResource(R.drawable.ic_phone_in_blue);
                Util.rotateImageButton(makeDiscoverableButton);
                textForMakeDiscoverableButton.setText(R.string.connecting);
                pinTextView.setVisibility(View.VISIBLE);
                break;
            case STREAMING:
                makeDiscoverableButton.setImageResource(R.drawable.ic_phone_in_green);
                makeDiscoverableButton.setAnimation(null);
                textForMakeDiscoverableButton.setText(R.string.connected);
                pinTextView.setVisibility(View.GONE);
                break;
            case RETRY:
                Util.rotateImageButton(makeDiscoverableButton);
                break;
            case FAILED:
            case STOPPED:
                makeDiscoverableButton.setImageResource(R.drawable.ic_phone_in_gray);
                makeDiscoverableButton.setAnimation(null);
                textForMakeDiscoverableButton.setText(R.string.not_connected);
                pinTextView.setVisibility(View.GONE);
                break;
            default:
                throw new RuntimeException("Unknown connection state " + connectionController.getState() + "!");
        }
    }

    private SoundPreset getCurrentSoundPreset() {
        // TODO: Get the current selection (which should be saved!)
//        Spinner presetSpinner = getView().findViewById(R.id.presetSpinner);
//        int position = presetSpinner.getSelectedItemPosition();
//        if ( position >= 0) {
//            return SoundPresetManager.getInstance(getContext()).getPreset(position);
//        }
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

    private void showPlaybackError(final String reason) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updatePlayButton();
                new AlertDialog.Builder(getContext())
                        .setTitle("Unable to play")
                        .setMessage(reason)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing...
                            }
                        })
                        .show();
            }
        });
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
                        // Should have a separate notification, so don't show anything here
                        // Will also get a STOPPED notification shortly, so no need to do anything else
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
    public void failed(String reason) {
        showPlaybackError(reason);
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