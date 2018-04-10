package peterson.ttu.edu.backupaids.sound;

import java.io.IOException;
import java.net.Socket;

/**
 * Class for recording audio and sending it over the network
 */
public class StreamRecording extends BaseSound {

    public StreamRecording(Socket socket) throws IOException {
        super(createMicAudioRecord(), createRemoteSoundDestination(socket));
    }
}
