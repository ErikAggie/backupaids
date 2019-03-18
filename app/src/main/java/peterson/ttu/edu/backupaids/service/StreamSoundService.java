package peterson.ttu.edu.backupaids.service;

import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.activities.TabbedMain;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;
import peterson.ttu.edu.backupaids.util.ConnectionType;

/**
 * Service for streaming sound to another device
 */
public class StreamSoundService extends BaseStreamService {
    private static final String TAG = "StreamSoundService";

    private static final int FOREGROUND_ID = 1235;
    private static final String STREAM_CHANNEL_NAME = "Stream Out";
    private static final AtomicBoolean currentlyStreaming = new AtomicBoolean(false);

    private final static List<Listener> listeners = new ArrayList<>();

    private static StreamSoundService instance = null;

    private boolean usedBluetooth = false;

    public static void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public StreamSoundService() {
        super("StreamSoundService",
               R.drawable.ic_stream_out,
              "Streaming audio",
              "Streaming audio to another device (FM style)",
               TabbedMain.class);
    }

    public synchronized static ConnectionMaker getConnectionMaker() {
        if ( instance == null) {
            return null;
        }
        return instance.connectionMaker;
    }

    @Override
    public boolean isRunning() {
        return currentlyStreaming.get();
    }

    @Override
    public void onDestroy() {
        synchronized(StreamSoundService.class) {
            instance = null;

            if ( usedBluetooth) {
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.stopBluetoothSco();
                usedBluetooth = false;
            }

            super.onDestroy();
        }
    }

    /**
     * Start looking for connections.
     * @param intent Intent arriving
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        synchronized(StreamSoundService.class) {
            currentlyStreaming.set(false); // This will stop the thread
            instance = this;
        }

        if (BluetoothMonitor.createIfNeeded(this).isHeadsetConnected()) {
            usedBluetooth = true;
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            audioManager.startBluetoothSco();
        }
        try {
            setUpService(intent, STREAM_CHANNEL_NAME, FOREGROUND_ID, ConnectionType.SPEAK);
        } catch (IOException e) {
            Log.w(TAG, "Unable to start streaming sound to another device: " + e.getMessage());
        }
    }

    @Override
    protected void streamingStarted() {
        synchronized(StreamSoundService.class) {
            currentlyStreaming.set(true);
        }
    }

    @Override
    protected void streamingStopped() {
        synchronized(StreamSoundService.class) {
            currentlyStreaming.set(false);
            instance = null;
        }
    }

    @Override
    protected SoundSource createSource(InputStream inputStream) throws IOException {
        return SourceFactory.createMicAudioRecord();
    }

    @Override
    protected SoundDestination createDestination(OutputStream outputStream) throws IOException {
        return DestinationFactory.createRemoteSoundDestination(outputStream);
    }

    @Override
    protected void noteError(String error) {
        for ( Listener listener : listeners) {
            listener.streamingErrored(error);
        }
    }


    public interface Listener {
        void streamingErrored(String error);
    }
}
