package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.service.BaseStreamService;
import peterson.ttu.edu.backupaids.service.RemoteSoundService;
import peterson.ttu.edu.backupaids.service.StreamSoundService;
import peterson.ttu.edu.backupaids.util.ConnectionType;

public class ConnectionController implements ConnectionMaker.ConnectionListener, BaseStreamService.Listener {

    public enum State {
        STARTUP,
        CONNECTING,
        STREAMING,
        FAILED,
        STOPPED;
    }

    private final Context context;
    private final Activity activity;
    private final ConnectionType connectionType;
    private final ConnectionMaker connectionMaker;
    private final Listener listener;

    private final Timer discoverableCountdown = new Timer();

    private State state = State.STARTUP;

    public ConnectionController(Context context, Activity activity, ConnectionType connectionType, Listener listener) {
        this.context = context;
        this.activity = activity;
        this.connectionType = connectionType;
        connectionMaker = new ConnectionMaker(context, this, connectionType);
        this.listener = listener;

        discoverableCountdown.schedule(new TimerTask() {
            @Override
            public void run() {
                if ( state == State.CONNECTING) {
                    // Took too long to connect
                    stop();
                }
            }
        }, 180000); // 3 minutes
    }

    public void stop() {
        switch ( connectionType) {
            case SPEAK:
                activity.stopService(new Intent(context, StreamSoundService.class));
                break;
            case LISTEN:
                activity.stopService(new Intent(context, RemoteSoundService.class));
                break;
            default:
                throw new RuntimeException("Unknown state: " + state);
        }
        connectionMaker.close();
        updateState(State.STOPPED);
    }

    private void updateState(State state) {
        if ( state != this.state) {
            // A change...
            this.state = state;
            listener.stateChanged(state);
        }
    }

    public State getState() {
        return state;
    }

    //----------------------------------------------------------------------
    // ConnectionMaker callbacks
    //----------------------------------------------------------------------
    @Override
    public void servicesStarted() {
        updateState(State.CONNECTING);
    }

    @Override
    public void servicesStopped() {
        updateState(State.STOPPED);
    }

    @Override
    public void servicePublishingFailed() {
        updateState(State.FAILED);
        stop();
    }

    @Override
    public void foundAPeer(String peerName) {
        // TODO: fill in
    }

    @Override
    public void findingPeerFailed(IOException e) {
        // TODO: fill in
    }

    @Override
    public void connectionFailed(IOException e) {
        // TODO: fill in
    }

    @Override
    public void connectionClosed() {
        // TODO: fill in
    }

    @Override
    public void connectionReady(Socket socket) throws IOException {
        // TODO: fill in
    }

    //----------------------------------------------------------------------------------
    // Service callbacks
    //----------------------------------------------------------------------------------
    @Override
    public void streamingStarted() {
        updateState(State.STREAMING);
    }

    // TODO: needed?
    @Override
    public void streamingFailed() {
        updateState(State.FAILED);
        stop();
    }

    @Override
    public void streamingStopped() {
        // TODO: this is where we would retry...
        stop();
    }

    public interface Listener {
        void stateChanged(State state);
    }
}
