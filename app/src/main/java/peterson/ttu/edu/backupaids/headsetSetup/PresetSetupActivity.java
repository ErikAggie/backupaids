package peterson.ttu.edu.backupaids.headsetSetup;

import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Spinner;

import peterson.ttu.edu.backupaids.R;

public class PresetSetupActivity extends FragmentActivity implements VolumeSetFragment.OnFragmentInteractionListener, FrequencyAdjustFragment.OnFragmentInteractionListener {

    private enum SetupSteps {
        Start("Start"), // This isn't a fragment, but a "-1" so that we can initialize to a non-existent fragment
        VolumeSet("VolumeSetFragment"),
        Hz125("Hz125Fragment"),
        Hz250("Hz250Fragment"),
        Hz500("Hz500Fragment"),
        Hz1000("Hz1000Fragment"),
        Hz2000("Hz2000Fragment"),
        Hz3000("Hz3000Fragment"),
        Hz4000("Hz4000Fragment"),
        Hz8000("Hz8000Fragment"),
        End("End"); // Another non-value to note that we're at the end

        private String mTag;

        SetupSteps(String tag) {
            mTag = tag;
        }

        public String getTag() { return mTag;}

        private static SetupSteps[] vals = values();
        public SetupSteps next()
        {
            return vals[(this.ordinal()+1) % vals.length];
        }
    }

    private SetupSteps currentStep = SetupSteps.Start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preset_setup);
        ProgressBar progressBar = findViewById(R.id.presetSetupProgress);
        progressBar.setMax(SetupSteps.values().length - 2);

        if ( savedInstanceState == null) {
            showNextFragment();
        }
    }

    private void showNextFragment() {
        if ( currentStep == SetupSteps.End || currentStep.next() == SetupSteps.End) {
            // We've reached the end. Complete the activity
            finish();
            return;
        }
        currentStep = currentStep.next();
        ProgressBar progressBar = findViewById(R.id.presetSetupProgress);
        progressBar.setProgress(currentStep.ordinal());

        switch ( currentStep)
        {
            case VolumeSet:
                showVolumeFragment();
                break;
        }
    }

    private void showVolumeFragment() {
        // TODO: use saved volume (if any)
        VolumeSetFragment volumeSetFragment = VolumeSetFragment.newInstance(-1);
        getSupportFragmentManager().beginTransaction().add(R.id.presetSetupFragmentLocation, volumeSetFragment).commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
