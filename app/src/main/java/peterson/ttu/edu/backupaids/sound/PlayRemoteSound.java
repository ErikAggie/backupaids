package peterson.ttu.edu.backupaids.sound;

import java.io.IOException;
import java.net.Socket;

import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Convenience class for playing sound from a remote headset
 */
public class PlayRemoteSound extends BaseSound {

    public PlayRemoteSound(Socket socket, SoundPreset preset) throws IOException {
        super(createStreamSource(socket), createLocalAudioDestination(preset));
    }
}
