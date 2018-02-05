package peterson.ttu.edu.backupaids.headsetSetup;

import android.content.Context;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import peterson.ttu.edu.backupaids.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VolumeSetFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VolumeSetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VolumeSetFragment extends DialogFragment {
    private static final String ARG_PRESET_NAME = "presetName";

    private String mPresetName;
    private boolean mPlaying = false;
    private AudioSettingObserver mVolumeObserver;

    private OnFragmentInteractionListener mListener;

    public VolumeSetFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param presetName The name of the preference (if it's being modified; null/empty otherwise
     * @return A new instance of fragment VolumeSetFragment.
     */
    public static VolumeSetFragment newInstance(String presetName) {
        VolumeSetFragment fragment = new VolumeSetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRESET_NAME, presetName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPresetName = getArguments().getString(ARG_PRESET_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.volume_setting, container, false);
    }


    public void startStopPlaying(View view) {
        if ( !mPlaying) {
            startPlaying();
        }
        else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlaying = true;
    }

    private void stopPlaying() {
        mPlaying = false;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mPercentageTextView = (TextView) getView().findViewById(R.id.volumeText);
        updateVolumePercentage();
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

        mVolumeObserver = new AudioSettingObserver(new Handler(), new IListenToVolumeChange() {
            @Override
            public void volumeChanged() {
                updateVolumePercentage();
            }
        });
        getContext().getApplicationContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mVolumeObserver);
    }

    private void updateVolumePercentage()
    {
        // Set the current volume (percentage)

        AudioManager audio = (AudioManager) this.getContext().getSystemService(Context.AUDIO_SERVICE);
        double currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        double maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int percentage = (int)(currentVolume / maxVolume * 100);
        TextView volumeText = getView().findViewById(R.id.volumeText);
        volumeText.setText(percentage + "%");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        if ( mVolumeObserver != null)
        {
            getContext().getApplicationContext().getContentResolver().unregisterContentObserver(mVolumeObserver);
            mVolumeObserver = null;
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
        void onFragmentInteraction(Uri uri);
    }
}
