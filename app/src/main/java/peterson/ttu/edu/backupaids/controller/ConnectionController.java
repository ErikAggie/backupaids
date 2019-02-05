package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.network.ConnectionListener;

public abstract class ConnectionController implements ConnectionListener, PeerCallback {

    private static final String TAG = "ConnectionController";

    public enum State {
        STARTUP,
        CONNECTING,
        STREAMING,
        FAILED,
        STOPPED
    }

    protected final Context context;
    protected final Activity activity;
    protected ConnectionMaker connectionMaker;
    private final Listener listener;

    private final Timer discoverableCountdown = new Timer();
    private volatile State state = State.STARTUP;

    private volatile boolean stopped = false;

    protected ConnectionController(Context context, Activity activity, Listener listener) throws IOException {
        this.context = context;
        this.activity = activity;
        this.listener = listener;
    }

    public synchronized ConnectionController start() throws IOException{
        connectionMaker = getConnectionMaker();
        return this;
    }

    public synchronized void stop() {
        if ( stopped) {
            return;
        }
        stopped = true;

        discoverableCountdown.cancel();

        stopService();
        if ( connectionMaker != null) {
            connectionMaker.close();
        }
        updateState(State.STOPPED);

    }

    protected abstract void stopService();

    protected synchronized void updateState(State state) {
        if ( state != this.state || state == State.STOPPED) {
            // A change...
            Log.d(TAG, "Connection state: " + state);
            this.state = state;
            listener.stateChanged(state);
        }
    }

    protected synchronized void failed(String reason) {
        Log.i(TAG, "Streaming failed: " + reason);
        if ( state == State.FAILED ||
             state == State.STOPPED) {
            // Already noted, or not important
            return;
        }
        listener.failed(reason);
        updateState(State.FAILED);

        stop();
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
    public void connectionDiscoveryFailed() {
        failed("Unable to set up a connection. Be sure the \"speak\" phone is looking for new connections while the \"listen\" phone is listening for new connections.");
    }

    @Override
    public void discoveryStarted() {
        listener.connectionCheckStarted();
    }

    @Override
    public void foundAPeer(String peerName) {
        listener.askAboutConnection(peerName, this);
    }

    @Override
    public void discoveryFinished() {
        listener.connectionCheckFinished();
    }

    @Override
    public void nowDiscoverable() {
        updateState(State.CONNECTING);
    }

    @Override
    public void noLongerDiscoverable() {
        if ( state == State.CONNECTING) {
            updateState(State.STOPPED);
        }
    }

    @Override
    public void findingPeerFailed(IOException e) {
        failed("Trouble finding another device. Please try again in a few seconds.");
    }

    @Override
    public void connectionReady() {
        startService();
        updateState(State.STREAMING);
    }

    protected abstract ConnectionMaker getConnectionMaker() throws IOException;

    protected abstract void startService();

    @Override
    public void connectionFailed(IOException e) {
        stop();
    }

    @Override
    public void connectionClosed() {
        if ( state == State.STOPPED) {
            // Duplicate
            return;
        }
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
        void failed(String reason);
        void connectionCheckStarted();
        void askAboutConnection(String connectionName, PeerCallback callback);
        void connectionCheckFinished();
    }
}
