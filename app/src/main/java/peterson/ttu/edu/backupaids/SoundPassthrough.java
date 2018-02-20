package peterson.ttu.edu.backupaids;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Plays sounds from the phone's microphones into the headset
 * Created by Erik Peterson on 1/30/2018.
 */

class SoundPassthrough {
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
        Log.i("Test", "Buffer size is " + bufferSize);
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
        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                Util.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

//        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.)

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                // TODO: record both if profile is headset...and the user wants it
                Log.i(TAG, "Bluetooth connected: " + i);
            }

            @Override
            public void onServiceDisconnected(int i) {
                // TODO: if this was our profile, stop recording with bluetooth mixed in
                Log.i(TAG, "Bluetooth disconnected: " + i);
            }
        };
        if ( !bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HEADSET)) {
            Log.e(TAG, "Unable to get bluetooth profile.");
        }
//        bluetoothAdapter.getProfileConnectionState(BluetoothAdapter.)
//
//        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_CALL,
//                Util.SAMPLE_RATE,
//                AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }

        final AudioTrack audioTrack = new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize)
                //.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "Audio playback won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }

        // Set up the audio effects
        if ( preset != null) {
            applyEffects(preset, audioTrack.getAudioSessionId());
        }

        playing = true;

        playRunnable = new Runnable() {
            @Override
            public void run()
            {
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
        };
        new Thread(playRunnable).start();
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
        }
    }

}
