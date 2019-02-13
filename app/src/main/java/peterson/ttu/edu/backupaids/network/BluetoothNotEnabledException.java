package peterson.ttu.edu.backupaids.network;

import java.io.IOException;

/**
 * Exception for when Bluetooth isn't enabled. It's a signal for the UI to request Bluetooth activation.
 */
public class BluetoothNotEnabledException extends IOException {

    /* package */ BluetoothNotEnabledException() {
        super("Bluetooth is not enabled; kindly ask the user to enable Bluetooth.");
    }
}
