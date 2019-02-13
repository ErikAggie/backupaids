package peterson.ttu.edu.backupaids.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Fragment for showing available connections
 */
public class ConnectionPopupFragment extends DialogFragment {

    private OnFragmentInteractionListener listener;
    private String availableConnection;

    public ConnectionPopupFragment() {
        // Required empty public constructor
    }

    public void setListener(OnFragmentInteractionListener listener) {
        this.listener = listener;
    }

    public void setConnection(String connection) {
        availableConnection = connection;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Connect?")
                .setMessage("Connect to " + availableConnection + "?")
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.connectionConfirmed();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.cancelled();
                    }
                });
        return builder.create();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    /* package */ interface OnFragmentInteractionListener {
        void connectionConfirmed();
        void cancelled();
    }
}
