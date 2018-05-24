package peterson.ttu.edu.backupaids.activities;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.service.LocalSoundService;
import peterson.ttu.edu.backupaids.service.StreamSoundService;

public class SpeakFragment extends Fragment implements View.OnClickListener, ConnectionManager.PeerListener, StreamSoundService.Listener {

    private ConnectionManager connectionManager;
    private final List<String> peers = new ArrayList<>();
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
        ourPin.setText(getString(R.string.our_pin, ConnectionManager.getPin()));

        // Listen for Bluetooth connections (for recording)
        IntentFilter connectFilter = new IntentFilter();
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        connectFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        getActivity().registerReceiver(BluetoothMonitor.createIfNeeded(getContext()), connectFilter);

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if ( isVisibleToUser) {
            // If we're already streaming (i.e. we've been woken up), find the existing connection manager
            if (StreamSoundService.isCurrentlyStreaming()) {
                connectionManager = ConnectionManager.getInstance();
            } else {
                connectionManager = new ConnectionManager(getContext(), this);
                Toast.makeText(getContext(), "Looking for other devices...this will take a few seconds.", Toast.LENGTH_LONG).show();
            }
            connectionManager.setPeerListener(this);
            StreamSoundService.registerListener(this);
            updatePlayButton();
        } else {
            if ( connectionManager != null) {
                // We're still looking for a connection, so stop...
                connectionManager.close();
                connectionManager = null;
            }
            StreamSoundService.unregisterListener(this);
        }
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(BluetoothMonitor.createIfNeeded(getContext()));
        if ( connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
        super.onDestroyView();
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
        if ( StreamSoundService.isCurrentlyStreaming()) {
            getActivity().stopService(new Intent(getContext(), StreamSoundService.class));
        } else if ( connectionManager != null) {
            // Ignore, we're still trying to connect
            Toast.makeText(getContext(), "Please be patient; I'll let you know when I've found a connection.", Toast.LENGTH_SHORT).show();
        } else {
            // User wants to restart
            connectionManager = new ConnectionManager(getContext(), this);
            StreamSoundService.registerListener(this);
            updatePlayButton();
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
                if (StreamSoundService.isCurrentlyStreaming()) {
                    playButton.setImageResource(R.drawable.ic_power_button_green);
                } else if ( connectionManager != null ){
                    playButton.setImageResource(R.drawable.ic_power_button_blue);
                } else {
                    // We're dead. Show button for restart
                    playButton.setImageResource(R.drawable.ic_refresh_black);
                }
            }
        });
    }

    @Override
    public void servicePublishingFailed() {
        Toast.makeText(getContext(), "Unable to make ourselves visible to other phones.", Toast.LENGTH_LONG).show();
        //finish();
    }

    @Override
    public void foundAPeer(String newPeer) {
        connectionPopup = new ConnectionPopupFragment();
        connectionPopup.setListener(new ConnectionPopupFragment.OnFragmentInteractionListener() {
            @Override
            public void connectionConfirmed(String connectionName) {
                // Let's get started!
                connectionManager.removePeerListener(SpeakFragment.this);
                Intent intent = new Intent(getContext(), StreamSoundService.class);
                intent.putExtra(Util.CONNECTION_NAME_EXTRA, connectionName);
                getActivity().startService(intent);
                // Service takes control of the connection manager
                connectionManager = null;
            }

            @Override
            public void cancelled() {
                connectionManager.close();
                updatePlayButton();
            }
        });
        connectionPopup.setTargetFragment(this, 1);
        connectionPopup.setConnection(newPeer);
        connectionPopup.show(getFragmentManager(), "Connections");
    }

    @Override
    public void findingPeerFailed(IOException e) {
        // TODO: Show something...
    }

    @Override
    public void connectionWaiting() {
        // Someone else is trying to connect (they got the popup). Start the service
        if ( connectionPopup != null) {
            connectionPopup.dismiss();
            connectionPopup = null;
        }
        connectionManager.removePeerListener(this);
        getActivity().startService(new Intent(getContext(), StreamSoundService.class));
        // Service takes control of ConnectionManager instance
        connectionManager = null;
    }

    @Override
    public void servicesStarted() {
        // Nothing for us to do
    }

    @Override
    public void servicesStopped() {
        updatePlayButton();
    }

    @Override
    public void connectionMade() {
        updatePlayButton();
    }

    @Override
    public void connectionFailed() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "Connection to the other device failed.", Toast.LENGTH_SHORT).show();
                updatePlayButton();
            }
        });
    }

    @Override
    public void connectionClosed() {
        if ( getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Connection to the other device was closed.", Toast.LENGTH_SHORT).show();
                    updatePlayButton();
                }
            });
        }
    }
}
