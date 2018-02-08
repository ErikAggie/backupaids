package peterson.ttu.edu.backupaids;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.os.Process;
import android.util.Log;

import java.io.IOException;

/**
 * Plays sounds from the phone's microphones into the headset
 * Created by Erik Peterson on 1/30/2018.
 */

class SoundPassthrough {
    private static final String TAG = "SoundPassthrough";

    private boolean playing = false;
    private int bufferSize;


    public SoundPassthrough()
    {
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

    public void start() throws IOException
    {
        if ( playing)
        {
            return;
        }
        playing = true;
        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                Util.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }


        final AudioTrack audioTrack = new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "Audio playback won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }

        // Set up the audio effects
        applyEffects(audioTrack.getAudioSessionId());

        new Thread(new Runnable() {
            @Override
            public void run()
            {
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
            }
        }).start();
    }

    /**
     * Apply effects to the audio session
     * @param audioSessionID Session ID to apply to
     */
    private void applyEffects(int audioSessionID)
    {
        final Equalizer equalizer = new Equalizer(1, audioSessionID);
        equalizer.setEnabled(true);

        /*new Thread(new Runnable() {
            public void run(){
                int numberOfBands = equalizer.getNumberOfBands();
                short[] values = {-1500, 0, 1500};
                try {
                    int i=0;
                    while ( playing)
                    {
                        i++;
                        for ( short band=0; band<numberOfBands; band++)
                        {
                            equalizer.setBandLevel(band, values[i%values.length]);
                        }
                        Thread.sleep(5000);
                    }

                } catch ( InterruptedException e) {

                }

            }
        }).start();*/
    }

    public void stop()
    {
        playing = false;
    }

}
