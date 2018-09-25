package peterson.ttu.edu.backupaids.sound.destination;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import peterson.ttu.edu.backupaids.util.Util;
import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Create various destination
 */
public class DestinationFactory {

    private static final String TAG = "DestinationFactory";

    /**
     * Create a local audio destination (audio track)
     * @return SoundDestination
     */
    public static SoundDestination createLocalAudioDestination(SoundPreset preset, Context context) throws IOException {
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        AudioTrack audioTrack;
        if ( Build.VERSION.SDK_INT >= 26) {
            // Android O contains a low-latency playback mode
            audioTrack = new AudioTrack.Builder().setAudioAttributes(audioAttributes)
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setBufferSizeInBytes(Util.INPUT_MIN_BUFFER_SIZE)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        } else {
            audioTrack = new AudioTrack.Builder().setAudioAttributes(audioAttributes)
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setBufferSizeInBytes(Util.INPUT_MIN_BUFFER_SIZE)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        }

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "Audio playback won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }

        // Set up the audio effects
        if ( preset != null) {
            AudioManager audioManager = context.getSystemService(AudioManager.class);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preset.getVolumeAdjust(), 0);
            applyEffects(preset, audioTrack.getAudioSessionId());
        }

        return new LocalSoundDestination(context, audioTrack, audioAttributes);
    }

    /**
     * For sending sound to a remote headset
     * @param socket Socket to write to
     */
    public static SoundDestination createRemoteSoundDestination(Socket socket) throws IOException {
        return new RemoteSoundDestination(socket);
    }


    /**
     * Apply effects to the audio session
     * @param audioSessionID Session ID to apply to
     */
    private static void applyEffects(SoundPreset preset, int audioSessionID)
    {
        final Equalizer equalizer = new Equalizer(1, audioSessionID);
        Map<Integer, Short> frequencyMap = preset.getFrequencyAdjustments();

        // Add the frequency adjustments one by one. Track which equalizer bands we're using
        // so we make sure the highest adjustment goes to each band
        Map<Short, Short> bandMap = new HashMap<>();

        for ( int frequency : frequencyMap.keySet()) {
            short adjustment = frequencyMap.get(frequency);
            short band = equalizer.getBand(frequency*1000); // Millihertz to hertz
            if ( bandMap.containsKey(band)) {
                if ( bandMap.get(band) < frequency) {
                    equalizer.setBandLevel(band, adjustment);
                    bandMap.put(band, adjustment);
                }
            } else {
                // First entry in this equalizer band
                equalizer.setBandLevel(band, adjustment);
                bandMap.put(band, adjustment);
            }
        }
        equalizer.setEnabled(true);
    }
}
