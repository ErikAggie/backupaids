package peterson.ttu.edu.backupaids.headsetSetup;

import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

import peterson.ttu.edu.backupaids.R;

public class PresetSetupActivity extends FragmentActivity implements VolumeSetFragment.OnFragmentInteractionListener, FrequencyAdjustFragment.OnFragmentInteractionListener {

    private enum SetupSteps {
        Start(-1), // This isn't a fragment, but a "-1" so that we can initialize to a non-existent fragment
        VolumeSet(0),
        Hz125(125),
        Hz250(250),
        Hz500(500),
        Hz1000(1000),
        Hz2000(2000),
        Hz3000(3000),
        Hz4000(4000),
        Hz8000(8000),
        End(-1); // Another non-value to note that we're at the end

        private int mFrequencyIfAny;

        SetupSteps(int frequency) {
            mFrequencyIfAny = frequency;
        }

        public int getFrequency() { return mFrequencyIfAny;}

        private static SetupSteps[] vals = values();
        public SetupSteps next()
        {
            return vals[(this.ordinal()+1) % vals.length];
        }
    }

    private static final String TAG = "PresetSetupActivity";

    private SetupSteps currentStep = SetupSteps.Start;
    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preset_setup);
        ProgressBar progressBar = findViewById(R.id.presetSetupProgress);
        progressBar.setMax(SetupSteps.values().length - 2);

        if ( savedInstanceState == null) {
            nextFragment();
        }
    }

    private void nextFragment() {
        if ( currentStep == SetupSteps.End || currentStep.next() == SetupSteps.End) {
            setupComplete();
            return;
        }
        currentStep = currentStep.next();
        ProgressBar progressBar = findViewById(R.id.presetSetupProgress);
        progressBar.setProgress(currentStep.ordinal());

        switch ( currentStep)
        {
            case Start:
                throw new RuntimeException("Shouldn't be on the START case...");
            case VolumeSet:
                showVolumeFragment();
                break;
            case Hz125:
            case Hz250:
            case Hz500:
            case Hz1000:
            case Hz2000:
            case Hz3000:
            case Hz4000:
            case Hz8000:
                showFrequencyFragment(currentStep);
                break;
            // End state is handled above
            default:
                throw new RuntimeException("Unexpected setup state " + currentStep);
        }
    }

    /**
     * Called when the setup is finished so we can save off the preset
     */
    private void setupComplete() {
        // TODO: Save off info from fragments...
        finish();
    }

    private void showVolumeFragment() {
        // TODO: use saved volume (if any)
        showFragment(VolumeSetFragment.newInstance(-1));
    }

    public void volumeAdjustmentComplete(int volumeLevel) {
        // TODO: save off the volume...
        nextFragment();
    }
    
    private void showFrequencyFragment(SetupSteps step) {
        // TODO: used saved value (if any)
        showFragment(FrequencyAdjustFragment.newInstance(step.getFrequency(), (short)0));
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if ( mCurrentFragment != null) {
            fragmentTransaction.remove(mCurrentFragment);
        }
        mCurrentFragment = fragment;
        fragmentTransaction.add(R.id.presetSetupFragmentLocation, mCurrentFragment);
        fragmentTransaction.commit();
    }

    public void frequencyAdjustmentComplete(short amount) {
        // TODO: save off the value...
        Log.i(TAG,"Frequency " + currentStep.getFrequency() + ": " + amount);
        nextFragment();
    }
}
