package peterson.ttu.edu.backupaids.sound.destination;

import java.io.IOException;

/**
 * Created by erika on 3/28/2018.
 */

public interface SoundDestination {
    void play();
    void write(byte[] data, int amount) throws IOException ;
    void stop() throws IOException;
}
