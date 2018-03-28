package peterson.ttu.edu.backupaids.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.sound.destination.LocalSoundDestination;
import peterson.ttu.edu.backupaids.sound.destination.RemoteSoundDestination;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.LocalSoundSource;
import peterson.ttu.edu.backupaids.sound.source.RemoteSoundSource;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;

/**
 * Base class for sounds. When created, it will automatically begin recording/playing/streaming
 */
public abstract class BaseSound {

    private static final String TAG = "BaseSound";

    protected static final int INPUT_MIN_BUFFER_SIZE =
            AudioRecord.getMinBufferSize(Util.SAMPLE_RATE,
                                         AudioFormat.CHANNEL_IN_MONO,
                                         AudioFormat.ENCODING_PCM_16BIT);
    // NOTE: don't think I ever used the minimum output buffer size...

    private boolean playing;

    private final SoundSource soundSource;
    private final SoundDestination soundDestination;

    /**
     * Constructor
     *
     * @param soundSource
     * @param soundDestination
     */
    public BaseSound(SoundSource soundSource, SoundDestination soundDestination) {
        this.soundSource = soundSource;
        this.soundDestination = soundDestination;

        if ( soundSource == null || soundDestination == null) {
            throw new RuntimeException("Must set a source and destination!");
        }
        playing = true;
        new Thread(new PlayAudio()).start();
    }

    /**
     * Stop what we're doing; won't return until playback has stopped
     */
    public void stop() {
        playing = false;
    }

    //----------------------------------------------------------------------------------------
    // Creating audio sources
    //----------------------------------------------------------------------------------------

    /**
     * Create a recorder for the camcorder (phone mics)
     *
     * @throws IOException
     */
    protected static SoundSource createCamcorderAudioRecord() throws IOException {
        AudioRecord audioRecord =
                new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                                Util.SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                INPUT_MIN_BUFFER_SIZE);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }
        return new LocalSoundSource(audioRecord);
    }

    /**
     * Create a recorder for the mic (i.e. whatever headset is plugged in/paired)
     *
     * @throws IOException
     */
    protected static SoundSource createMicAudioRecord() throws IOException {
//        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//        audioManager.startBluetoothSco();
        AudioRecord audioRecord =
                new AudioRecord(MediaRecorder.AudioSource.MIC,
                                Util.SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                INPUT_MIN_BUFFER_SIZE);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }
        return new LocalSoundSource(audioRecord);
    }

    /**
     * Create a sound source for a socket
     * @param socket Socket we're getting sound from
     */
    protected static SoundSource createStreamSource(Socket socket) throws IOException {
        return new RemoteSoundSource(socket);
    }

    //--------------------------------------------------------------------------------------------
    // Creating audio destinations
    //--------------------------------------------------------------------------------------------

    /**
     * Create a local audio destination (audio track)
     * @return SoundDestination
     */
    protected static SoundDestination createLocalAudioDestination(SoundPreset preset) throws IOException {
        AudioTrack audioTrack;
        if ( Build.VERSION.SDK_INT >= 26) {
            // Android O contains a low-latency playback mode
            audioTrack = new AudioTrack.Builder().setAudioAttributes(
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(INPUT_MIN_BUFFER_SIZE)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        } else {
            audioTrack = new AudioTrack.Builder().setAudioAttributes(
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(INPUT_MIN_BUFFER_SIZE)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        }

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "Audio playback won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }

        // Set up the audio effects
        if ( preset != null) {
            applyEffects(preset, audioTrack.getAudioSessionId());
        }

        return new LocalSoundDestination(audioTrack);
    }

    /**
     * For sending sound to a remote headset
     * @param socket
     */
    protected static SoundDestination createRemoteSoundDestination(Socket socket) throws IOException {
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

    private class PlayAudio implements Runnable {

        @Override
        public void run(){
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                // Short buffer would be half of the buffer size; byte buffer is the full size
                byte[] audioBuffer = new byte[INPUT_MIN_BUFFER_SIZE];

                soundSource.record();
                soundDestination.play();

                // Here's the playing loop!
                while (playing) {
                    int amountRead = soundSource.read(audioBuffer);
                    soundDestination.write(audioBuffer, amountRead);
                }
            } catch ( IOException e) {
                Log.e(TAG, "Error playing audio: ", e);
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
    }
}
