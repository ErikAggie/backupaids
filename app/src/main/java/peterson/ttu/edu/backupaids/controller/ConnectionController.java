package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import peterson.ttu.edu.backupaids.network.ConnectionMaker;

public abstract class ConnectionController implements ConnectionMaker.ConnectionListener, PeerCallback {

    private static final String TAG = "ConnectionController";

    private static final int LENGTH_TO_WAIT_FOR_RECONNECT = 3000; // 3 seconds
    private static final int LENGTH_OF_REPEAT_FAILURE_TIMEOUT = 120000; // two minutes

    private static final int MAX_FAILURES_IN_TWO_MINUTES = 2;

    public enum State {
        STARTUP,
        CONNECTING,
        STREAMING,
        RETRY,
        FAILED,
        STOPPED;
    }

    protected final Context context;
    protected final Activity activity;
    protected final ConnectionMaker connectionMaker;
    private final Listener listener;

    private final Timer discoverableCountdown = new Timer();
    private State state = State.STARTUP;

    private AtomicInteger numRecentFailures = new AtomicInteger(0);
    private Timer repeatFailureTimer = new Timer();
    private String savedApplicationName;

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
        repeatFailureTimer.cancel();

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

    protected void failed(String reason) {
        if ( state == State.FAILED ||
             state == State.STOPPED) {
            // Already noted, or not important
            return;
        }
        listener.failed(reason);
        updateState(State.FAILED);
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
        if ( state == State.STOPPED) {
            return;
        }
        int numFailures = numRecentFailures.incrementAndGet();
        if ( numFailures > MAX_FAILURES_IN_TWO_MINUTES) {
            stop();
        } else if ( savedApplicationName == null) {
            updateState(State.RETRY);
            // We are the listener, so the other device might try to reconnect.
            // Set up a short timer to give him a chance to reconnect
            Log.i(TAG, "Connection lost; giving sender time to reconnect...");
            repeatFailureTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if ( state == State.STREAMING)
                        numRecentFailures.set(0);
                }
            }, LENGTH_TO_WAIT_FOR_RECONNECT);
        } else {
            Log.i(TAG, "Attempting to reconnect...");
            updateState(State.RETRY);
            connectionMaker.makeConnection(savedApplicationName);

            // Restart the timer
            repeatFailureTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if ( state == State.STREAMING)
                    numRecentFailures.set(0);
                }
            }, LENGTH_OF_REPEAT_FAILURE_TIMEOUT);
        }
    }

    //----------------------------------------------------------------------------------
    // Connection approval/deny callbacks
    //----------------------------------------------------------------------------------

    @Override
    public void approveConnection(String connectionName) {
        savedApplicationName = connectionName;
        // No need to start the service yet (like it was done before this class came into being
        connectionMaker.makeConnection(connectionName);
    }

    @Override
    public void denyConnection(String connectionName) {
        stop();
    }

    public interface Listener {
        void stateChanged(State state);
        void failed(String Reason);
        void askAboutConnection(String connectionName, PeerCallback callback);
    }
}
