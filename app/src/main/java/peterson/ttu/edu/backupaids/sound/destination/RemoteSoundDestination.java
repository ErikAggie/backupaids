package peterson.ttu.edu.backupaids.sound.destination;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class for sending sound data over a socket
 */
public class RemoteSoundDestination implements SoundDestination {

    private final OutputStream outputStream;

    public RemoteSoundDestination(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void play() {
    }

    @Override
    public void write(byte[] data, int amount) throws IOException {
        outputStream.write(data, 0, amount);
        outputStream.flush();
    }

    @Override
    public boolean hasStopped() {
        return false;
    }

    @Override
    public void stop() throws IOException {
        outputStream.close();
    }
}
