package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import peterson.ttu.edu.backupaids.network.ConnectionMaker;

public abstract class ConnectionController implements ConnectionMaker.ConnectionListener, PeerCallback {

    public enum State {
        STARTUP,
        CONNECTING,
        STREAMING,
        FAILED,
        STOPPED;
    }

    protected final Context context;
    protected final Activity activity;
    protected final ConnectionMaker connectionMaker;
    private final Listener listener;

    private final Timer discoverableCountdown = new Timer();

    private State state = State.STARTUP;

    protected ConnectionController(Context context, Activity activity, Listener listener) {
        this.context = context;
        this.activity = activity;
        this.listener = listener;

        connectionMaker = getConnectionMaker();
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
        stopService();
        connectionMaker.close();
        updateState(State.STOPPED);

    }

    protected abstract void stopService();

    protected void updateState(State state) {
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
    public void connectionReady(int connectionNumber) {
        startService(connectionNumber);
        updateState(State.STREAMING);
    }

    protected abstract ConnectionMaker getConnectionMaker();

    protected abstract void startService(int connectionNumber);

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

    public interface Listener {
        void stateChanged(State state);
        void askAboutConnection(String connectionName, PeerCallback callback);
    }
}
