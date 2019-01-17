package peterson.ttu.edu.backupaids.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.controller.ConnectionController;
import peterson.ttu.edu.backupaids.controller.PeerCallback;
import peterson.ttu.edu.backupaids.controller.SpeakConnectionController;
import peterson.ttu.edu.backupaids.network.BluetoothNotEnabledException;
import peterson.ttu.edu.backupaids.util.Util;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.service.StreamSoundService;

public class SpeakFragment extends Fragment implements View.OnClickListener, ConnectionController.Listener {

    private static final int REQUEST_ENABLE_BT = 1;

    private ConnectionController connectionController;
    private ConnectionPopupFragment connectionPopup;

    //----------------------------------------------------------------------------------------------
    // Lifecycle methods
    //----------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Apparently we'll think we're visible at first...
        setUserVisibleHint(false);
        View fragmentView = inflater.inflate(R.layout.fragment_speak, container, false);

        ImageButton sendSoundStartButton = fragmentView.findViewById(R.id.sendSoundStartButton);
        sendSoundStartButton.setOnClickListener(this);

        return fragmentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //setContentView(R.layout.fragment_speak);

        TextView ourPin = getView().findViewById(R.id.sendSoundOurPinTextView);
        ourPin.setText(getString(R.string.our_pin, ConnectionMaker.OUR_PIN));

        // Listen for Bluetooth connections (for recording)
        IntentFilter connectFilter = new IntentFilter();
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        connectFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        getActivity().registerReceiver(BluetoothMonitor.createIfNeeded(getContext()), connectFilter);

        updatePlayButton(view);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if ( isVisibleToUser) {
            if (ContextCompat.checkSelfPermission(getActivity(), TabbedMain.Permission.REQUEST_COARSE_LOCATION_PERMISSION.getName())
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                Activity activity = getActivity();
                if ( !(activity instanceof TabbedMain)) {
                    throw new RuntimeException("Must be under a TabbedMain!");
                }
                final TabbedMain tabbedMain = ((TabbedMain) activity);
                tabbedMain.requestPermission(TabbedMain.Permission.REQUEST_COARSE_LOCATION_PERMISSION, new TabbedMain.PermissionCallback() {
                    @Override
                    public void permissionGranted() {
                        startConnectionController();
                    }

                    @Override
                    public void permissionDenied() {
                        tabbedMain.showListenTab();
                    }
                });
            } else {
                // If we're already streaming (i.e. we've been woken up), find the existing connection manager
                startConnectionController();
            }
        } else {
            if ( connectionController != null && connectionController.getState() != ConnectionController.State.STREAMING) {
                stopTryingToConnect();
            }
        }
    }

    private void stopTryingToConnect() {
        if ( connectionController!= null) {
            connectionController.stop();
        }
        // Play button will get updated by a status update
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(BluetoothMonitor.createIfNeeded(getContext()));
        stopTryingToConnect();
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if ( resultCode == Activity.RESULT_OK) {
                    // Restart the controller
                    startConnectionController();
                } else {
                    Toast.makeText(getContext(), "Unable to connect without Bluetooth", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                throw new RuntimeException("Unknown SpeakFragment request code: " + requestCode);
        }
    }

    //----------------------------------------------------------------------------------------------
    // UI events
    //----------------------------------------------------------------------------------------------

    @Override
    public void onClick(View view) {
        switch ( view.getId()) {
            case R.id.sendSoundStartButton:
                sendSound();
                break;
            default:
                throw new RuntimeException("Unexpected button push!");
        }
    }

    private void sendSound() {
        if ( connectionController == null) {
            startConnectionController();
        } else {
            connectionController.stop();
        }
    }

    private void startConnectionController() {
        try {
            connectionController = new SpeakConnectionController(getContext(), getActivity(), this);
            if (!StreamSoundService.isCurrentlyStreaming()) {
                Toast.makeText(getContext(), "Looking for other devices...this will take a few seconds.", Toast.LENGTH_LONG).show();
            }
        } catch (BluetoothNotEnabledException ex) {
            // Bluetooth isn't running. Ask the user to start it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } catch ( IOException ex) {
            Toast.makeText(getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //----------------------------------------------------------------------------------------------
    // Helper methods
    //----------------------------------------------------------------------------------------------

    private void updatePlayButton() {
        if ( getView() != null) {
            updatePlayButton(getView());
        }
    }

    private void updatePlayButton(View view) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton playButton = getView().findViewById(R.id.sendSoundStartButton);
                if ( connectionController == null) {
                    playButton.setImageResource(R.drawable.ic_refresh_gray);
                } else {
                    switch (connectionController.getState()) {
                        case STARTUP:
                        case CONNECTING:
                            playButton.setImageResource(R.drawable.ic_power_button_blue);
                            Util.rotateImageButton(playButton);
                            break;
                        case STREAMING:
                            playButton.setImageResource(R.drawable.ic_power_button_green);
                            playButton.setAnimation(null);
                            break;
                        case STOPPED:
                        case FAILED:
                            playButton.setImageResource(R.drawable.ic_refresh_gray);
                            playButton.setAnimation(null);
                            break;
                        default:
                            throw new RuntimeException("Unknown state " + connectionController.getState());
                    }
                }
            }
        });
    }


    @Override
    public void stateChanged(final ConnectionController.State state) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updatePlayButton();
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
                        connectionController = null;
                        break;
                    default:
                        // Nothing to do...
                }
            }
        });
    }

    @Override
    public void failed(final String reason) {
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

        // This isn't likely (at present--September, 2018--this is only for headphone not present--not a problem here
        Toast.makeText(getContext(), "Connection to the other device failed.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void askAboutConnections(final List<String> connectionNames, final PeerCallback callback) {
        CharSequence[] connectionNamesAsArray = new CharSequence[connectionNames.size()];
        connectionNames.toArray(connectionNamesAsArray);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select device to connect to");
        builder.setItems(connectionNamesAsArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                callback.approveConnection(connectionNames.get(i));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                TabbedMain activity = (TabbedMain) getActivity();
                activity.showListenTab();
            }
        });
        builder.show();

        // Leftovers from the WiFi settings

//        if ( connectionPopup != null) {
//            connectionPopup.dismiss();
//        }
//        connectionPopup = new ConnectionPopupFragment();
//        connectionPopup.setListener(new ConnectionPopupFragment.OnFragmentInteractionListener() {
//            @Override
//            public void connectionConfirmed() {
//                callback.approveConnection(connectionName);
//            }
//
//            @Override
//            public void cancelled() {
//                callback.denyConnection(connectionName);
//                if (connectionPopup != null) {
//                    connectionPopup.dismiss();
//                    connectionPopup = null;
//                }
//            }
//        });
//        connectionPopup.setTargetFragment(this, 1);
//        connectionPopup.setConnection(connectionName);
//        connectionPopup.show(getFragmentManager(), "Connections");

    }
}
