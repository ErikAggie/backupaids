package peterson.ttu.edu.backupaids.sound.destination;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for playing sound locally (speakers/headphones)
 */
public class LocalSoundDestination implements SoundDestination, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "LocalSoundDestination";

    private static AudioFocusRequest audioFocusRequest;
    private static LocalSoundDestination requestingSoundDestination;

    private final Context context;
    private final AudioTrack audioTrack;
    private final AudioAttributes audioAttributes;
    private boolean stopped = false;

    private static final AtomicInteger numberPlaying = new AtomicInteger(0);

    private final AtomicBoolean paused = new AtomicBoolean(false);

    public LocalSoundDestination(Context context, AudioTrack audioTrack, AudioAttributes audioAttributes) {
        this.context = context;
        this.audioTrack = audioTrack;
        this.audioAttributes = audioAttributes;
    }

    @Override
    public void play() throws IOException {
        AudioManager audioManager = context.getSystemService(AudioManager.class);

        if ( numberPlaying.getAndIncrement() == 0) {

            if ( Build.VERSION.SDK_INT >= 26) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(audioAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener(this)
                        .build();
                if ( audioManager.requestAudioFocus(audioFocusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    // We didn't get audio focus. No point in continuing
                    numberPlaying.decrementAndGet();
                    audioFocusRequest = null;
                    throw new IOException("Unable to get audio focus!");
                }
            } else {
                int res = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                requestingSoundDestination = this;
                if ( res != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    // We didn't get audio focus. No point in continuing
                    numberPlaying.decrementAndGet();
                    requestingSoundDestination = null;
                    throw new IOException("Unable to get audio focus!");
                }
            }
        }

        audioTrack.play();
    }

    @Override
    public void write(byte[] data, int amount) {
        if ( !paused.get()) {
            audioTrack.write(data, 0, amount);
        }
    }

    @Override
    public boolean hasStopped() {
        return stopped;
    }

    @Override
    public void stop() {
        stopped = true;
        int numRemainingPlayers = numberPlaying.decrementAndGet();
        if ( numRemainingPlayers <= 0) {
            AudioManager audioManager = context.getSystemService(AudioManager.class);
            if (  Build.VERSION.SDK_INT >= 26) {
                if ( audioFocusRequest != null) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                    audioFocusRequest = null;
                }
            } else if ( requestingSoundDestination != null) {
                audioManager.abandonAudioFocus(requestingSoundDestination);
                requestingSoundDestination = null;
            }
        }
        audioTrack.stop();
        audioTrack.release();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch(focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus completely
                Log.i(TAG, "Lost audio focus");
                stopped = true;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                paused.set(true);
                audioTrack.pause();
                audioTrack.flush();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                paused.set(false);
                audioTrack.play();
                break;
            default:
                Log.e(TAG, "Unexpected audio event: " + focusChange);
        }
    }
}
