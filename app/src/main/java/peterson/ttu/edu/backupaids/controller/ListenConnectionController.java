package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.service.BaseStreamService;
import peterson.ttu.edu.backupaids.service.RemoteSoundService;
import peterson.ttu.edu.backupaids.util.ConnectionType;

public class ListenConnectionController extends ConnectionController implements RemoteSoundService.Listener {

    public ListenConnectionController(Context context, Activity activity, Listener listener) {
        super(context, activity, listener);
    }

    @Override
    protected ConnectionMaker getConnectionMaker() {
        if ( RemoteSoundService.isCurrentlyStreaming()) {
            // We're already connected; so just hook up with what's there
            ConnectionMaker connectionMaker = RemoteSoundService.getConnectionMaker();
            connectionMaker.changeConnectionListener(this);
            updateState(State.STREAMING);
            return connectionMaker;
        } else {
            return new ConnectionMaker(context, this, ConnectionType.LISTEN);
        }
    }

    @Override
    public void startService(int connectionNumber) {
        RemoteSoundService.registerListener(this);
        Intent startIntent = new Intent(context, RemoteSoundService.class);
        startIntent.putExtra(BaseStreamService.CONNECTION_NUMBER_EXTRA, connectionNumber);
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
