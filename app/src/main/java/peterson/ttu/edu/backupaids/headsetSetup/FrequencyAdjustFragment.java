package peterson.ttu.edu.backupaids.headsetSetup;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import peterson.ttu.edu.backupaids.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FrequencyAdjustFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FrequencyAdjustFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FrequencyAdjustFragment extends DialogFragment implements View.OnClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_BAND_FREQUENCY = "argBandFrequency";
    private static final String ARG_BAND_ADJUSTMENT = "argBandAdjustment";

    private int mBandFrequency;
    private int mBandAdjustment;

    private OnFragmentInteractionListener mListener;

    public FrequencyAdjustFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bandFrequency Frequency we're adjusting
     * @param bandAdjustment Initial adjustment (default is 0)
     * @return A new instance of fragment FrequencyAdjustFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FrequencyAdjustFragment newInstance(int bandFrequency, int bandAdjustment) {
        FrequencyAdjustFragment fragment = new FrequencyAdjustFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BAND_FREQUENCY, bandFrequency);
        args.putInt(ARG_BAND_ADJUSTMENT, bandAdjustment);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBandFrequency = getArguments().getInt(ARG_BAND_FREQUENCY);
            mBandAdjustment = getArguments().getInt(ARG_BAND_ADJUSTMENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_frequency_adjust, container, false);

        // Put our band in the title
        TextView titleView = fragmentView.findViewById(R.id.frequencyAdjustTitle);
        final String newText = titleView.getText().toString() + mBandFrequency;
        titleView.setText(newText);

        return fragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {

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
        void onFragmentInteraction(Uri uri);
    }
}
