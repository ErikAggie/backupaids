package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.io.IOException;

import peterson.ttu.edu.backupaids.model.DeviceInfo;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.network.ConnectionMakerFactory;
import peterson.ttu.edu.backupaids.service.RemoteSoundService;
import peterson.ttu.edu.backupaids.util.ConnectionType;

public class ListenConnectionController extends ConnectionController implements RemoteSoundService.Listener {

    private final boolean makeVisible;

    public ListenConnectionController(@NonNull Context context, @NonNull Activity activity, @NonNull Listener listener, boolean makeVisible) throws IOException {
        super(context, activity, listener);
        this.makeVisible = makeVisible;
    }

    @Override
    protected ConnectionMaker getConnectionMaker() throws IOException {
        if ( RemoteSoundService.isCurrentlyStreaming()) {
            // We're already connected; so just hook up with what's there
            ConnectionMaker connectionMaker = RemoteSoundService.getConnectionMaker();
            connectionMaker.changeConnectionListener(this);
            updateState(State.STREAMING);
            return connectionMaker;
        } else if (makeVisible){
            return ConnectionMakerFactory.createConnectionMaker(context, this, ConnectionType.LISTEN);
        } else {
            // Just listen
            updateState(State.CONNECTING);
            return ConnectionMakerFactory.createConnectionMakerNoDiscovery(context, this, ConnectionType.LISTEN);
        }
    }

    @Override
    public void startService() {
        RemoteSoundService.registerListener(this);
        Intent startIntent = new Intent(context, RemoteSoundService.class);
        activity.startService(startIntent);
    }

    @Override
    protected void stopService() {
        RemoteSoundService.unregisterListener(this);
        activity.stopService(new Intent(context, RemoteSoundService.class));
    }

    @Override
    public void playbackErrored(String error) {
        failed(error);
    }
}
