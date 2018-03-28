package peterson.ttu.edu.backupaids.sound.destination;

import android.media.AudioTrack;

/**
 * Class for playing sound locally (speakers/headphones)
 */
public class LocalSoundDestination implements SoundDestination {

    private final AudioTrack audioTrack;

    public LocalSoundDestination(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    @Override
    public void play() {
        audioTrack.play();
    }

    @Override
    public void write(byte[] data, int amount) {
        audioTrack.write(data, 0, amount);
    }

    @Override
    public void stop() {
        audioTrack.stop();
        audioTrack.release();
    }
}
