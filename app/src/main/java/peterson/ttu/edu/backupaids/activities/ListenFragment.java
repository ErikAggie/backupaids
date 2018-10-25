package peterson.ttu.edu.backupaids.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.controller.ConnectionController;
import peterson.ttu.edu.backupaids.controller.ListenConnectionController;
import peterson.ttu.edu.backupaids.controller.PeerCallback;
import peterson.ttu.edu.backupaids.activities.headsetSetup.PresetSetupActivity;
import peterson.ttu.edu.backupaids.model.Preferences;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.service.LocalSoundService;
import peterson.ttu.edu.backupaids.service.RemoteSoundService;
import peterson.ttu.edu.backupaids.util.Util;

public class ListenFragment extends Fragment implements View.OnClickListener, ConnectionController.Listener, Preferences.PresetUpdateListener, PresetListAdapter.ButtonListener {

    private static final String TAG = "ListenFragment";

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

        Preferences.getInstance(getContext()).addPresetUpdateListener(this);

        ImageButton playButton = fragmentView.findViewById(R.id.playSound);
        playButton.setOnClickListener(this);

        ImageButton micToUseButton = fragmentView.findViewById(R.id.micToUseButton);
        micToUseButton.setOnClickListener(this);
        TextView micToUseText = fragmentView.findViewById(R.id.micToUseText);
        micToUseText.setOnClickListener(this);

        ImageButton makeDiscoverableButton = fragmentView.findViewById(R.id.makeDiscoverable);
        makeDiscoverableButton.setOnClickListener(this);
        TextView makeDiscoverableText = fragmentView.findViewById(R.id.textForMakeDiscoverableButton);
        makeDiscoverableText.setOnClickListener(this);

        ImageButton presetChooserButton = fragmentView.findViewById(R.id.presetChooser);
        presetChooserButton.setOnClickListener(this);
        TextView presetChooserText = fragmentView.findViewById(R.id.textForPresetChooser);
        presetChooserText.setOnClickListener(this);

        return fragmentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView ourPin = getView().findViewById(R.id.ourPinTextView);
        ourPin.setText(getString(R.string.our_pin, ConnectionMaker.getPin()));

        updatePresetViewer();

        if ( RemoteSoundService.isCurrentlyStreaming()) {
            // Already streaming. Need to re-connect with this guy
            connectionController = new ListenConnectionController(getContext(), getActivity(), this);
        }

        // We could already be playing, so check on that...
        updatePlayButton();
        updateMicToUseButton();
        updateMakeDiscoverableButton();
    }


    @Override
    public void onDestroyView() {
        stopStreaming();
        stopPlaying();

        Preferences.getInstance(getContext()).removePresetUpdateListner(this);

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
            case R.id.micToUseButton:
            case R.id.micToUseText:
                switchMic();
                break;
            case R.id.presetChooser:
            case R.id.textForPresetChooser:
                editPresets();
                break;
            case R.id.makeDiscoverable:
            case R.id.textForMakeDiscoverableButton:
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

    private void switchMic() {
        Preferences preferences = Preferences.getInstance(getContext());
        Preferences.MicToUse micToUse = preferences.getMicToUse();

        switch(micToUse) {
            case PHONE_MIC:
                preferences.setMicToUse(Preferences.MicToUse.HEADSET_MIC);
                break;
            case HEADSET_MIC:
                preferences.setMicToUse(Preferences.MicToUse.PHONE_MIC);
                break;
            default:
                throw new RuntimeException("Unknown mic type " + micToUse);
        }

        updateMicToUseButton();
    }

    private void editPresets() {
        if (SoundPresetManager.getInstance(getContext()).getNumberOfPresets() <= 0) {
            // We need to make a new preset
            createNewPreset();
            return;
        }

        // Show the current presets, with edit/delete buttons (and a new one)
        // Some of the code here came from https://stackoverflow.com/questions/23464232/how-would-you-create-a-popover-view-in-android-like-facebook-comments
        LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View inflatedView = layoutInflater.inflate(R.layout.preset_popup, null,false);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Make it square along the dimension of the
        int sideLength = (int)(Math.min(size.x, size.y) * .8);

        final PopupWindow popupWindow = new PopupWindow(inflatedView, sideLength, sideLength);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(getContext().getDrawable(R.drawable.popup_drawable));

        ListView presetPopupListView = inflatedView.findViewById(R.id.presetPopupListView);
        presetPopupListView.setAdapter(
                new PresetListAdapter(popupWindow,
                        this,
                        getContext(),
                        R.layout.preset_popup_list_item,
                        SoundPresetManager.getInstance(getContext()).getAllSortedPresets()));

        Button closeButton = inflatedView.findViewById(R.id.presetPopupClose);
        closeButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                }
        );

        Button newPresetButton = inflatedView.findViewById(R.id.presetPopupNewPresetButton);
        newPresetButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                        createNewPreset();
                    }
                }
        );

        TableLayout bottomPanel = getView().findViewById(R.id.bottomMenu);
        bottomPanel.getTop();
        int[] position = new int[2];
        bottomPanel.getLocationOnScreen(position);

        popupWindow.showAtLocation(bottomPanel, Gravity.BOTTOM + Gravity.RIGHT, 0, size.y-bottomPanel.getTop());
    }

    private void createNewPreset() {
        AudioManager audioManager = getActivity().getApplicationContext().getSystemService(AudioManager.class);
        if ( !Util.areHeadphonesActive(audioManager)) {
            showPlaybackError(getString(R.string.headphones_needed));
            return;
        }
        startActivityForResult(new Intent(getContext(), PresetSetupActivity.class), 0);
        // TODO: figure out how to get the result; if a new preset was finished, select it
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

    private void updatePresetViewer() {
        TextView textForPresetChooser = getView().findViewById(R.id.textForPresetChooser);
        SoundPreset preset = Preferences.getInstance(getContext()).getSelectedPreset();
        if ( preset != null) {
            textForPresetChooser.setText(preset.getName());
        } else {
            textForPresetChooser.setText(R.string.no_preset_selected);
        }
    }

    private void updatePlayButton() {
        ImageButton playButton = getView().findViewById(R.id.playSound);
        if ( LocalSoundService.isLocalSoundServiceRunning()) {
            playButton.setImageResource(R.drawable.ic_power_button_green);
        } else {
            playButton.setImageResource(R.drawable.ic_power_button_blue);
        }
    }

    private void updateMicToUseButton() {
        Preferences preferences = Preferences.getInstance(getContext());
        Preferences.MicToUse micToUse = preferences.getMicToUse();
        ImageButton micToUseButton = getView().findViewById(R.id.micToUseButton);
        TextView micToUseText = getView().findViewById(R.id.micToUseText);

        switch(micToUse) {
            case PHONE_MIC:
                micToUseButton.setImageResource(R.drawable.ic_phone_mic_blue);
                micToUseText.setText(R.string.using_phone_mic);
                break;
            case HEADSET_MIC:
                micToUseButton.setImageResource(R.drawable.ic_headset_blue);
                micToUseText.setText(R.string.using_headset_mic);
                break;
            default:
                throw new RuntimeException("Unknown mic type " + micToUse);
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
        updatePresetViewer();
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

    @Override
    public void selectedPresetUpdated(String newPresetName) {
        updatePresetViewer();
    }

    @Override
    public void deletePresetButtonPushed(PopupWindow popupWindow, SoundPreset soundPreset) {
        SoundPresetManager.getInstance(getContext()).deletePreset(soundPreset);
        updatePresetViewer();

        // Replace the adapter since the preset list has changed
        ListView presetListView = popupWindow.getContentView().findViewById(R.id.presetPopupListView);
        presetListView.setAdapter(
                new PresetListAdapter(popupWindow,
                        this,
                        getContext(),
                        R.layout.preset_popup_list_item,
                        SoundPresetManager.getInstance(getContext()).getAllSortedPresets()));
    }
}