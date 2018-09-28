package peterson.ttu.edu.backupaids.sound.destination;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import peterson.ttu.edu.backupaids.service.ServiceSetupException;

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

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.i(TAG, "Becoming noisy (headphones removed, etc.), so stopping playback.");
                stop();
            }
        }
    };

    public LocalSoundDestination(Context context, AudioTrack audioTrack, AudioAttributes audioAttributes) {
        this.context = context;
        this.audioTrack = audioTrack;
        this.audioAttributes = audioAttributes;
    }

    @Override
    public void play() throws ServiceSetupException, IOException {

        AudioManager audioManager = context.getApplicationContext().getSystemService(AudioManager.class);

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        context.getApplicationContext().registerReceiver(becomingNoisyReceiver, intentFilter);

        // We wait until now to check for headphones because it might take a while to get here
        // (e.g. connecting to a phone takes a few seconds, so we don't want to assume headphones
        // are attached immediately)
        if ( !areHeadphonesActive(audioManager)) {
            throw new ServiceSetupException("Please insert headphones before listening. Otherwise you're likely to get a horrible screeching noise. :)");
        }

        if ( numberPlaying.getAndIncrement() == 0) {

            if ( Build.VERSION.SDK_INT >= 26) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(audioAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener(this)
                        .setWillPauseWhenDucked(true)
                        .build();
                if ( audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.i(TAG, "Gained audio focus!");
                } else {
                    // We didn't get audio focus. No point in continuing
                    numberPlaying.decrementAndGet();
                    audioFocusRequest = null;
                    throw new IOException("Unable to play back (likely due to a phone call)");
                }
            } else {
                // Pre-Oreo
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

    private boolean areHeadphonesActive(AudioManager audioManager) {
        AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for ( AudioDeviceInfo audioDevice : audioDevices) {
            switch(audioDevice.getType()) {
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                case AudioDeviceInfo.TYPE_LINE_ANALOG:
                case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                    return true;
            }
        }
        return false;
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
        if ( stopped) {
            return;
        }
        stopped = true;
        context.getApplicationContext().unregisterReceiver(becomingNoisyReceiver);
        int numRemainingPlayers = numberPlaying.decrementAndGet();
        if ( numRemainingPlayers <= 0) {
            AudioManager audioManager = context.getApplicationContext().getSystemService(AudioManager.class);
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
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.i(TAG, "Audio focus temporarily lost. Pausing...");
                paused.set(true);
                audioTrack.pause();
                audioTrack.flush();
                // Note that we will keep listening to data, but won't play any of it...
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
