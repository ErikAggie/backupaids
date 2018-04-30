package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.activities.MainActivity;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

/**
 * Service for playing local sound.
 */
public class LocalSoundService extends IntentService {

    private static final String TAG = "LocalSoundService";
    private static final int FOREGROUND_ID = 1234;
    private static final String PLAYBACK_CHANNEL_NAME = "Playback";

    private static boolean running;
    private static String preset;

    private boolean playing = true;

    private final static List<Listener> listeners = new ArrayList<>();

    public LocalSoundService() {
        super("LocalSoundService");
    }

    public static boolean isRunning() {
        return running;
    }

    public static void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public static String getCurrentPreset() {
        return preset;
    }

    public static void setCurrentPreset(String newPreset) {
        preset = newPreset;
    }

    @Override
    public void onDestroy() {
        // This will tell the thread to stop
        playing = false;


        for ( Listener listener : listeners) {
            listener.playbackStopped();
        }

        super.onDestroy();
    }


    /**
     * Do the playback/stream (read from the source and play at the destination)
     * @param intent Intent that started us
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SoundPreset soundPreset = null;
        if ( preset != null && !preset.isEmpty()) {
            soundPreset = SoundPresetManager.getInstance(this).getPreset(preset);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        if ( Build.VERSION.SDK_INT >= 26) {
            // Create the notification channel needed to show this notification...
            NotificationChannel channel = new NotificationChannel(PLAYBACK_CHANNEL_NAME, PLAYBACK_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, PLAYBACK_CHANNEL_NAME)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle("Playing mic audio")
                .setContentText("Playing audio from the phone's microphones")
                .setContentIntent(pendingIntent);

        startForeground(FOREGROUND_ID, notificationBuilder.build());

        running = true;

        for ( Listener listener : listeners) {
            listener.playbackStarted();
        }

        SoundSource soundSource = null;
        SoundDestination soundDestination = null;

        try {
            soundSource = SourceFactory.createCamcorderAudioRecord();
            // TODO: find current sound source (key/value store)
            soundDestination = DestinationFactory.createLocalAudioDestination(soundPreset);
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            // Short buffer would be half of the buffer size; byte buffer is the full size
            byte[] audioBuffer = new byte[Util.INPUT_MIN_BUFFER_SIZE];

            soundSource.record();
            soundDestination.play();

            // Here's the playing loop!
            while (playing) {
                int amountRead = soundSource.read(audioBuffer);
                if (amountRead < 0) {
                    break;
                }
                soundDestination.write(audioBuffer, amountRead);
            }
        } catch ( Exception e) {
            Log.w(TAG, "Stopping playback/streaming: " + e.getMessage());
        } finally {
            running = false;

            for ( Listener listener : listeners) {
                listener.playbackStopped();
            }

            // Clean up
            try {
                if ( soundSource != null) {
                    soundSource.stop();
                }
            } catch ( IOException e) {
                // Nothing to do
            }
            try {
                if ( soundDestination != null) {
                    soundDestination.stop();
                }
            } catch ( IOException e) {
                // Nothing to do
            }
        }
    }

    public interface Listener {
        void playbackStarted();
        void playbackStopped();
    }
}
