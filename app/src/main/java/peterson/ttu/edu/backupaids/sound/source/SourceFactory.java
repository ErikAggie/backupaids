package peterson.ttu.edu.backupaids.sound.source;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import peterson.ttu.edu.backupaids.model.Preferences;
import peterson.ttu.edu.backupaids.util.Util;

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
    public static SoundSource createCamcorderAudioRecord(Context context) throws IOException {
        AudioRecord audioRecord =
                new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                        Util.LOCAL_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        Util.LOCAL_MIN_BUFFER_SIZE);

        Preferences.MicToUse micToUse = Preferences.getInstance(context).getMicToUse();
        switch (micToUse) {
            case PHONE_MIC:
                // Force use of the phone's microphones
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                for ( AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                    if ( device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                        audioRecord.setPreferredDevice(device);
                        break;
                    }
                }
                break;
            case HEADSET_MIC:
                // Nothing to do; this ought to be the default
                break;
            default:
                throw new RuntimeException("Unknown mic type " + micToUse);
        }
        Log.i(TAG, "Audio device is " + audioRecord.getPreferredDevice());
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
                        Util.REMOTE_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        Util.REMOTE_MIN_BUFFER_SIZE);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record won't initialize!");
            throw new IOException("Audio Record won't initialize!");
        }
        AudioFormat format = audioRecord.getFormat();
        Log.i(TAG, "Audio encoding: " + format.getEncoding());
        Log.i(TAG, "Sample rate: " + format.getSampleRate());
        return new LocalSoundSource(audioRecord);
    }

    /**
     * Create a sound source for a socket
     * @param inputStream Source of the sound
     */
    public static SoundSource createStreamSource(InputStream inputStream) {
        return new RemoteSoundSource(inputStream);
    }


}
