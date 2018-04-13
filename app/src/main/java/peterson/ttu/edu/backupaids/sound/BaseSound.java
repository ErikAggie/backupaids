package peterson.ttu.edu.backupaids.sound;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;

import java.io.IOException;

import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;

/**
 * Base class for sounds. When created, it will automatically begin recording/playing/streaming
 */
public class BaseSound extends Service {

    private boolean playing;

    private final SoundSource soundSource;
    private final SoundDestination soundDestination;

    /**
     * Constructor
     *  @param soundSource Sound source
     * @param soundDestination Where sound is going
     */
    public BaseSound(SoundSource soundSource, SoundDestination soundDestination) {
        this.soundSource = soundSource;
        this.soundDestination = soundDestination;

        if ( soundSource == null || soundDestination == null) {
            throw new RuntimeException("Must set a source and destination!");
        }
        playing = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        playing = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Do the work!
     * @throws IOException
     */
    public void playAudio() throws IOException {
        try {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            // Short buffer would be half of the buffer size; byte buffer is the full size
            byte[] audioBuffer = new byte[Util.INPUT_MIN_BUFFER_SIZE];

            soundSource.record();
            soundDestination.play();

            // Here's the playing loop!
            while (playing) {
                int amountRead = soundSource.read(audioBuffer);
                if ( amountRead < 0) {
                    break;
                }
                soundDestination.write(audioBuffer, amountRead);
            }
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
        playing = false;
    }
}
