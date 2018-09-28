package peterson.ttu.edu.backupaids.sound.destination;

import java.io.IOException;

import peterson.ttu.edu.backupaids.service.ServiceSetupException;

/**
 * Created by erika on 3/28/2018.
 */

public interface SoundDestination {
    void play() throws ServiceSetupException, IOException;
    void write(byte[] data, int amount) throws IOException ;
    boolean hasStopped();
    void stop() throws IOException;
}
