package peterson.ttu.edu.backupaids.sound.source;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;

import peterson.ttu.edu.backupaids.Util;

/**
 * Create various sources
 */
public class SourceFactory {

    private static final String TAG = "SourceFactory";

    /**
     * Create a recorder for the camcorder (phone mics)
     *
     * @throws IOException If we can't initialize recording
     */
    public static SoundSource createCamcorderAudioRecord() throws IOException {
        AudioRecord audioRecord =
                new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                        Util.SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        Util.INPUT_MIN_BUFFER_SIZE);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }
        return new LocalSoundSource(audioRecord);
    }


    /**
     * Create a recorder for the mic (i.e. whatever headset is plugged in/paired)
     *
     * @throws IOException If se can't initialize the recording
     */
    public static SoundSource createMicAudioRecord() throws IOException {
        AudioRecord audioRecord =
                new AudioRecord(MediaRecorder.AudioSource.MIC,
                        Util.SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        Util.INPUT_MIN_BUFFER_SIZE);
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
    public static SoundSource createStreamSource(Socket socket) throws IOException {
        return new RemoteSoundSource(socket);
    }


}
