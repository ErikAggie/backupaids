package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.service.BaseStreamService;
import peterson.ttu.edu.backupaids.service.RemoteSoundService;
import peterson.ttu.edu.backupaids.service.StreamSoundService;
import peterson.ttu.edu.backupaids.util.ConnectionType;

public abstract class ConnectionController implements ConnectionMaker.ConnectionListener, BaseStreamService.Listener, PeerCallback {

    public enum State {
        STARTUP,
        CONNECTING,
        STREAMING,
        FAILED,
        STOPPED;
    }

    protected final Context context;
    protected final Activity activity;
    private final ConnectionMaker connectionMaker;
    private final Listener listener;

    private final Timer discoverableCountdown = new Timer();

    private State state = State.STARTUP;

    public ConnectionController(Context context, Activity activity, ConnectionType connectionType, Listener listener) {
        this.context = context;
        this.activity = activity;
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
        discoverableCountdown.cancel();
        stopActivity();
        connectionMaker.close();
        updateState(State.STOPPED);

    }

    protected abstract void stopActivity();

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
        listener.askAboutConnection(peerName, this);
    }


    @Override
    public void findingPeerFailed(IOException e) {
        updateState(State.FAILED);
        stop();
    }

    @Override
    public abstract void connectionReady(int connectionNumber);

    @Override
    public void connectionFailed(IOException e) {
        stop();
    }

    @Override
    public void connectionClosed() {
        stop();
    }

    //----------------------------------------------------------------------------------
    // Connection approval/deny callbacks
    //----------------------------------------------------------------------------------

    @Override
    public void approveConnection(String connectionName) {
        // No need to start the service yet (like it was done before this class came into being
        connectionMaker.makeConnection(connectionName);
    }

    @Override
    public void denyConnection(String connectionName) {
        stop();
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
        // TODO: this is where we would retry?
        stop();
    }

    public interface Listener {
        void stateChanged(State state);
        void askAboutConnection(String connectionName, PeerCallback callback);
    }
}
