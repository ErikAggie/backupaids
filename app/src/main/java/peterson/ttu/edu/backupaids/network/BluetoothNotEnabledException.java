package peterson.ttu.edu.backupaids.network;

import java.io.IOException;

public class BluetoothNotEnabledException extends IOException {

    /* package */ BluetoothNotEnabledException(String message) {
        super(message);
    }
}
