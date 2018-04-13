package peterson.ttu.edu.backupaids.sound;

import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;

/**
 * Base class for sounds. When created, it will automatically begin recording/playing/streaming
 */
public class SoundService extends IntentService {

    private static final String TAG = "SoundService";

    private boolean playing;

    private SoundSource soundSource;
    private SoundDestination soundDestination;
    private Notification notification;

    public SoundService() {
        super("SoundService");

    }

    public void setSoundSource(SoundSource soundSource) {
        this.soundSource = soundSource;
    }

    public void setSoundDestination(SoundDestination soundDestination) {
        this.soundDestination = soundDestination;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if ( soundSource == null) {
            throw new NullPointerException("Cannot start a sound service without a source.");
        }
        if ( soundDestination == null) {
            throw new NullPointerException("Cannot start a sound service without a destination");
        }
        if ( notification == null) {
            throw new NullPointerException("Cannot start a sound service without a notification");
        }
        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        playing = false;
        super.onDestroy();
    }

    /**
     * Do the playback/stream (read from the source and play at the destination)
     * @param intent Intent that started us
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
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

            // Clean up
            try {
                soundSource.stop();
            } catch ( IOException e) {
                // Nothing to do
            }
            try {
                soundDestination.stop();
            } catch ( IOException e) {
                // Nothing to do
            }
        }
    }

    /**
     * Stop what we're doing
     */
    public void stop() {
        stopSelf();
        playing = false;
    }
}
