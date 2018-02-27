package peterson.ttu.edu.backupaids.sound;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Plays sounds from the phone's microphones into the headset
 * Created by Erik Peterson on 1/30/2018.
 */

public class SoundPassthrough {
    private static final String TAG = "SoundPassthrough";

    private Context context;
    private Runnable playRunnable;
    private boolean playing = false;
    private boolean threadRunning = false;
    private Object notifyObject = new Object();
    private int bufferSize;


    public SoundPassthrough(Context context)
    {
        this.context = context;
        int inputMinBufferSize = AudioRecord.getMinBufferSize(Util.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        int outputMinBufferSize = AudioTrack.getMinBufferSize(Util.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if ( inputMinBufferSize <= 0 || outputMinBufferSize <= 0)
        {
            Log.e("Playback", "Buffer size not specified. In: " + inputMinBufferSize + ", Out: " + outputMinBufferSize);
            bufferSize = Util.SAMPLE_RATE * 2;
        }
        else
        {
            bufferSize = inputMinBufferSize;
        }
    }

    public boolean isPlaying()
    {
        return playing;
    }

    public void start(SoundPreset preset) throws IOException
    {
        if ( playing)
        {
            stop();
        }

        playing = true;
//        if ( BluetoothMonitor.getInstance().isHeadsetConnected()) {
//            new Thread(new RunSingleInput(createBluetoothAudioRecord(), createAudioTrack(preset))).start();
//        }
        new Thread(new RunSingleInput(createHeadsetAudioRecord(), createAudioTrack(preset))).start();
    }

    private AudioRecord createHeadsetAudioRecord() throws IOException {
        AudioRecord audioRecord =
                new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                        Util.SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }
        return audioRecord;
    }

    private AudioRecord createBluetoothAudioRecord() throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.startBluetoothSco();
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                Util.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }
        return audioRecord;
    }

    private AudioTrack createAudioTrack(SoundPreset preset) throws IOException {
        AudioTrack audioTrack;
        if ( Build.VERSION.SDK_INT >= 26) {
            // Android O contains a low-latency playback mode
            audioTrack = new AudioTrack.Builder().setAudioAttributes(
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(bufferSize)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        } else {
            audioTrack = new AudioTrack.Builder().setAudioAttributes(
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(bufferSize)
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

        return audioTrack;
    }

    private class RunSingleInput implements Runnable {

        private final AudioRecord audioRecord;
        private final AudioTrack audioTrack;

        public RunSingleInput(AudioRecord audioRecord, AudioTrack audioTrack) {
            this.audioRecord = audioRecord;
            this.audioTrack = audioTrack;
        }

        @Override
        public void run() {
            threadRunning = true;
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            short[] audioBuffer = new short[bufferSize / 2];

            audioTrack.play();

            audioRecord.startRecording();

            // Here's the playing loop!
            while ( playing)
            {
                int amountRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                audioTrack.write(audioBuffer, 0, amountRead);
            }

            // Clean up
            audioRecord.stop();
            audioRecord.release();
            audioTrack.stop();
            audioTrack.release();
            synchronized (notifyObject) {
                threadRunning = false;
                notifyObject.notify();
            }
        }
    }

    private class RunTwoInputs implements Runnable {

        private final AudioRecord audioRecord1;
        private final AudioRecord audioRecord2;
        private final AudioTrack audioTrack;

        public RunTwoInputs(AudioRecord audioRecord1, AudioRecord audioRecord2, AudioTrack audioTrack) {
            this.audioRecord1 = audioRecord1;
            this.audioRecord2 = audioRecord2;
            this.audioTrack = audioTrack;
        }

        @Override
        public void run() {
            threadRunning = true;
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            short[] audioBuffer1 = new short[bufferSize / 2];
            short[] audioBuffer2 = new short[bufferSize / 2];
            short[] outputBuffer = new short[bufferSize / 2];

            audioTrack.play();

            audioRecord1.startRecording();
            audioRecord2.startRecording();

            // Here's the playing loop!
            while ( playing)
            {
                int amountRead = audioRecord1.read(audioBuffer1, 0, audioBuffer1.length);
                int amountRead2 = audioRecord2.read(audioBuffer2, 0, audioBuffer2.length);
                //Log.i(TAG, "Read " + amountRead + " vs." + amountRead2);
                // We'll drop stuff that we couldn't read
                amountRead = Math.max(amountRead, amountRead2);
                // Need this to be even for the code below...
                if ( amountRead % 2 == 1) {
                    amountRead++;
                }
                int largeResult;
                // Since this is a time-critical piece, I did some optimizing:
                //   1. Doing things two at a time is ~11% faster than incrementing by 1
                //   2. The Math.min(...Math.max(...)) is significantly faster than trying to short-circuit the process
                //      (perhaps the compiler is inlining it)
                for ( int index=0, nextIndex = 1; index<amountRead; index+=2, nextIndex += 2) {
                    largeResult = audioBuffer1[index] + audioBuffer2[index];
                    outputBuffer[index] = (short)Math.min(Short.MAX_VALUE, (Math.max(largeResult, Short.MIN_VALUE)));
                    largeResult = audioBuffer1[nextIndex] + audioBuffer2[nextIndex];
                    outputBuffer[nextIndex] = (short)Math.min(Short.MAX_VALUE, (Math.max(largeResult, Short.MIN_VALUE)));
                }

                audioTrack.write(outputBuffer, 0, amountRead);
            }

            // Clean up
            audioRecord1.stop();
            audioRecord1.release();
            audioRecord2.stop();
            audioRecord2.release();
            // We know we're using Bluetooth here...
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.stopBluetoothSco();
            audioTrack.stop();
            audioTrack.release();
            synchronized (notifyObject) {
                threadRunning = false;
                notifyObject.notify();
            }

        }
    }

    /**
     * Apply effects to the audio session
     * @param audioSessionID Session ID to apply to
     */
    private void applyEffects(SoundPreset preset, int audioSessionID)
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

    public void stop()
    {
        playing = false;
        if ( playRunnable != null) {
            // This really shouldn't be done on the GUI thread, but it shouldn't take long
            // for the other thread to stop
            if ( threadRunning ) {
                synchronized (notifyObject) {
                    if ( threadRunning) {
                        try {
                            notifyObject.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // Stop the SCO session, if any
            if ( BluetoothMonitor.getInstance().isHeadsetConnected()) {
                // TODO: use a variable to determine if we STARTED using a Bluetooth device
            }
        }
    }

}
