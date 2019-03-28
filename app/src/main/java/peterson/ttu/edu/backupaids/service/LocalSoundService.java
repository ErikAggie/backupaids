package peterson.ttu.edu.backupaids.service;

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
import java.util.concurrent.atomic.AtomicBoolean;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.model.Preferences;
import peterson.ttu.edu.backupaids.util.Util;
import peterson.ttu.edu.backupaids.activities.TabbedMain;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

/**
 * Service for playing local sound.
 */
public class LocalSoundService extends BaseService implements ServiceBroadcastReceiver.Callback {

    private static final String TAG = "LocalSoundService";
    // Used so ServiceBroadcastReceiver can direct intents to us
    private static final String SERVICE_NAME = LocalSoundService.class.getCanonicalName();
    private static final int FOREGROUND_ID = 1234;
    private static final String PLAYBACK_CHANNEL_NAME = "Playback";
    private static final String ACTION_STOP = "peterson.ttu.edu.backupaids.service.LocalSoundService.StopLocalPlayback";

    private static final AtomicBoolean running = new AtomicBoolean(false);

    private boolean playing = true;

    private final static List<Listener> listeners = new ArrayList<>();

    public LocalSoundService() {
        super("LocalSoundService");
    }

    public static boolean isLocalSoundServiceRunning() {
        return running.get();
    }

    public static void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onDestroy() {
        unregisterThisService();

        // This will tell the thread to stopRequested
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

        registerThisService();

        SoundPreset soundPreset = Preferences.getInstance(this).getSelectedPreset();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, TabbedMain.class), 0);

        if ( Build.VERSION.SDK_INT >= 26) {
            // Create the notification channel needed to show this notification...
            NotificationChannel channel = new NotificationChannel(PLAYBACK_CHANNEL_NAME, PLAYBACK_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }

        SoundSource soundSource = null;
        SoundDestination soundDestination = null;

        try {
            ServiceBroadcastReceiver.registerCallback(SERVICE_NAME, this);
            soundSource = SourceFactory.createCamcorderAudioRecord(this);
            // TODO: find current sound source (key/value store)
            soundDestination = DestinationFactory.createLocalAudioDestination(soundPreset, this, false);
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            // Short buffer would be half of the buffer size; byte buffer is the full size
            byte[] audioBuffer = new byte[Util.LOCAL_MIN_BUFFER_SIZE];

            soundSource.record();
            soundDestination.play();

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, PLAYBACK_CHANNEL_NAME)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_play)
                    .setContentTitle("Playing mic audio")
                    .setContentText("Playing audio from the phone's microphones")
                    .setContentIntent(pendingIntent);

            Intent stopIntent = new Intent(this, ServiceBroadcastReceiver.class);
            stopIntent.putExtra(ServiceBroadcastReceiver.SERVICE_NAME_EXTRA, SERVICE_NAME);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.addAction(R.drawable.ic_stop_black_24dp, "Stop", stopPendingIntent);

            startForeground(FOREGROUND_ID, notificationBuilder.build());

            running.set(true);

            for ( Listener listener : listeners) {
                listener.playbackStarted();
            }

            // Here's the playing loop!
            while (playing) {
                if (soundDestination.hasStopped()) {
                    break;
                }
                int amountRead = soundSource.read(audioBuffer);
                if (amountRead < 0) {
                    break;
                }
                soundDestination.write(audioBuffer, amountRead);
            }
        } catch ( ServiceSetupException e) {
            running.set(false);
            Log.w(TAG, "Setup error: " + e.getMessage());
            for ( Listener listener : listeners) {
                listener.playbackErrored(e.getMessage());
            }
        } catch ( IOException e) {
            Log.w(TAG, "Stopping playback: " + e.getMessage());
        } finally {
            running.set(false);

            ServiceBroadcastReceiver.unregisterCallback(SERVICE_NAME);

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

    @Override
    public boolean isRunning() {
        return isLocalSoundServiceRunning();
    }

    @Override
    protected void stopNow() {
        running.set(false);
        for ( Listener listener : listeners) {
            listener.playbackStopped();
        }
        stopSelf();
    }

    @Override
    public void stopRequested() {
        stopNow();
    }

    public interface Listener {
        void playbackStarted();
        void playbackStopped();
        void playbackErrored(String error);
    }
}
