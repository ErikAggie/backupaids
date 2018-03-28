package peterson.ttu.edu.backupaids.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import peterson.ttu.edu.backupaids.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ConnectionPopupFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConnectionPopupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectionPopupFragment extends DialogFragment {

    public static final String CONNECTIONS_ARGUMENT = "connections";

    private OnFragmentInteractionListener mListener;
    private String[] connectionArray;

    private String selectedConnection;

    public ConnectionPopupFragment() {
        // Required empty public constructor
    }

    public void setConnectionList(List<String> connectionList) {
        connectionArray = new String[connectionList.size()];
        for ( int i=0; i<connectionList.size(); i++) {
            connectionArray[i] = connectionList.get(i);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Available connections")
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.connectionConfirmed(selectedConnection);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.cancelled();
                    }
                });
        builder.setSingleChoiceItems(connectionArray, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                selectedConnection = connectionArray[which];
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void connectionConfirmed(String connectionName);
        void cancelled();
    }
}
