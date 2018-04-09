package peterson.ttu.edu.backupaids.sound.source;

import android.media.AudioRecord;

import java.io.IOException;

/**
 * Sound coming from a local source (phone mics or connected headset)
 */
public class LocalSoundSource implements SoundSource {

    private final AudioRecord audioRecord;

    public LocalSoundSource(AudioRecord audioRecord) {
        this.audioRecord = audioRecord;
    }

    @Override
    public void record() throws IOException {
        audioRecord.startRecording();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return audioRecord.read(buffer, 0, buffer.length);
    }

    @Override
    public void stop() throws IOException {
        audioRecord.stop();
        audioRecord.release();
    }
}
