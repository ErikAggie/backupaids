package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.activities.TabbedMain;
import peterson.ttu.edu.backupaids.model.Preferences;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;
import peterson.ttu.edu.backupaids.util.ConnectionType;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class RemoteSoundService extends BaseStreamService implements ServiceNotificationCallback {

    private static final String TAG = "RemoteSoundService";

    private static final int FOREGROUND_ID = 1236;
    private static final String STREAM_CHANNEL_NAME = "Stream In";

    private final static List<Listener> listeners = new ArrayList<>();

    private static final AtomicBoolean currentlyStreaming = new AtomicBoolean(false);

    public static void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public static boolean isCurrentlyStreaming() {
        return currentlyStreaming.get();
    }

    private static RemoteSoundService instance;

    /**
     * Returns the current ConnectionMaker. If we aren't running, this will be null;
     * @return The ConnectionMaker we're using
     */
    public static ConnectionMaker getConnectionMaker() {
        if ( instance == null) {
            throw new RuntimeException("No instance available!");
        }
        return instance.connectionMaker;
    }

    public RemoteSoundService() {
        super("RemoteSoundService",
               R.drawable.ic_stream_in,
              "Streaming audio",
              "Streaming audio from another device (FM style)",
               TabbedMain.class);
        RemotePlaybackBroadcastReceiver.registerCallback(this);
    }

    @Override
    public void onDestroy() {
        currentlyStreaming.set(false); // This will stop the thread
        instance = null;

        super.onDestroy();
    }

    @Override
    public boolean isRunning() {
        return isCurrentlyStreaming();
    }

    @Override
    protected void stopNow() {
        currentlyStreaming.set(false);
        stopSelf();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            setUpService(intent, STREAM_CHANNEL_NAME, FOREGROUND_ID, ConnectionType.LISTEN);
        } catch ( IOException e) {
            // Not sure what to do here besides log
            Log.e(TAG, "Error setting up services: " + e.getMessage(), e);
        }
    }

    @Override
    protected void streamingStarted() {
        instance = this;
        currentlyStreaming.set(true);
    }

    @Override
    protected void streamingStopped() {
        currentlyStreaming.set(false);
        instance = null;
        RemotePlaybackBroadcastReceiver.unregisterCallback();
    }

    @Override
    protected SoundSource createSource(InputStream inputStream) {
        return SourceFactory.createStreamSource(inputStream);
    }

    @Override
    protected SoundDestination createDestination(OutputStream outputStream) throws IOException {
        SoundPreset soundPreset = Preferences.getInstance(this).getSelectedPreset();

        return DestinationFactory.createLocalAudioDestination(soundPreset, this, true);
    }

    @Override
    protected void noteError(String error) {
        for ( Listener listener : listeners) {
            listener.playbackErrored(error);
        }
    }

    @Override
    public void stopRequested() {
        stopNow();
    }

    public interface Listener {
        void playbackErrored(String error);
    }

}
