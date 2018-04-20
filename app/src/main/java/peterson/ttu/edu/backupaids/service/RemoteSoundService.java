package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class RemoteSoundService extends IntentService {

    public RemoteSoundService() {
        super("RemoteSoundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if ( intent == null) {
            return;
        }

        final String connectionName = intent.getStringExtra("ConnectionName");

        if ( connectionName == null || connectionName.isEmpty()) {
            throw new RuntimeException("Cannot start RemoteSoundService without a connection name!");
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
