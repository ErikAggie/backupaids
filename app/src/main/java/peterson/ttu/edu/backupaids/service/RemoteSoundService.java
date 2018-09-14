package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.content.Intent;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.activities.TabbedMain;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class RemoteSoundService extends BaseStreamService {

    private static final String TAG = "RemoteSoundService";

    private static final int FOREGROUND_ID = 1236;
    private static final String STREAM_CHANNEL_NAME = "Stream In";

    private static final AtomicBoolean currentlyStreaming = new AtomicBoolean(false);
    private static String preset;

    public static String getCurrentPreset() {
        return preset;
    }

    public static void setCurrentPreset(String newPreset) {
        preset = newPreset;
    }

    public static boolean isCurrentlyStreaming() {
        return currentlyStreaming.get();
    }

    private static RemoteSoundService instance;

    private Timer killTimer = new Timer();

    /**
     * Returns the current ConnectionMaker. If we aren't running, this will be null;
     * @return
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
    protected void onHandleIntent(Intent intent) {
        setUpService(intent, STREAM_CHANNEL_NAME, FOREGROUND_ID);
    }

    @Override
    protected void streamingStarted() {
        instance = this;
        currentlyStreaming.set(true);

//        killTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                stopStreaming();
//            }
//        }, 10000);
    }

    @Override
    protected void streamingStopped() {
        currentlyStreaming.set(false);
        instance = null;
    }

    @Override
    protected SoundSource createSource(Socket socket) throws IOException {
        return SourceFactory.createStreamSource(socket);
    }

    @Override
    protected SoundDestination createDestination(Socket socket) throws IOException {
        SoundPreset soundPreset = null;
        if ( preset != null && !preset.isEmpty()) {
            soundPreset = SoundPresetManager.getInstance(this).getPreset(preset);
        }

        return DestinationFactory.createLocalAudioDestination(soundPreset, this);
    }
}
