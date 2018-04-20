package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.activities.SendSoundActivity;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

/**
 * Service for streaming sound to another device
 */
public class StreamSoundService extends BaseStreamService {

    private static final String TAG = "StreamSoundService";

    private static final int FOREGROUND_ID = 1235;
    private static final String STREAM_CHANNEL_NAME = "Stream Out";

    private static boolean currentlyStreaming = false;
    private static final List<BaseStreamService.Listener> listeners = new ArrayList<>();

    public static boolean isCurrentlyStreaming() {
        return currentlyStreaming;
    }

    public static void registerListener(BaseStreamService.Listener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(BaseStreamService.Listener listener) {
        listeners.remove(listener);
    }

    public StreamSoundService() {
        super("StreamSoundService");
    }

    @Override
    public void onDestroy() {
        currentlyStreaming = false; // This will stop the thread

        super.onDestroy();
    }

    /**
     * Start looking for connections.
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        setUpService(intent, STREAM_CHANNEL_NAME, FOREGROUND_ID);
    }

    @Override
    protected void streamingStarted() {
        currentlyStreaming = true;
    }

    @Override
    protected void streamingStopped() {
        currentlyStreaming = false;
    }

    @Override
    protected List<Listener> getListeners() {
        return listeners;
    }

    @Override
    protected SoundSource createSource(Socket socket) throws IOException{
        return SourceFactory.createMicAudioRecord();
    }

    @Override
    protected SoundDestination createDestination(Socket socket) throws IOException{
        return DestinationFactory.createRemoteSoundDestination(socket);
    }
}
