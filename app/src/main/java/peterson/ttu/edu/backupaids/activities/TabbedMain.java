package peterson.ttu.edu.backupaids.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.service.StreamSoundService;
import peterson.ttu.edu.backupaids.util.Util;

public class TabbedMain extends AppCompatActivity {

    private static final String TAG = "TabbedMain";

    public enum Permission {
        REQUEST_RECORD_AUDIO_PERMISSION(200,
                Manifest.permission.RECORD_AUDIO,
                "This app needs to access your microphone in order to listen to your surroundings. " +
                        "This app will not store or send audio data without your permission. " +
                        "Please say \"Allow\" on the following screen."),
        REQUEST_COARSE_LOCATION_PERMISSION(201,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                "In order to find other devices Android requires the use of your general location. " +
                        "This app will not store or share your location; it's purely to connect to another device. " +
                        "If that's okay with you, please say \"Allow\" on the following screen.");

        private final int id;
        private final String name;
        private final String message;

        Permission(int id, String name, String message) {
            this.id = id;
            this.name = name;
            this.message = message;
        }

        public String getName() {
            return name;
        }
    }

    /* package */ interface PermissionCallback {
        void permissionGranted();
        void permissionDenied();
    }

    private final SparseArray<PermissionCallback> permissionCallbackMap = new SparseArray<>();

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private TabLayout tabLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed_main);

        Util.setUpSampleRates((AudioManager) this.getSystemService(Context.AUDIO_SERVICE));
        if ( !Util.canSendData()) {
            // We can't send remote audio
            findViewById(R.id.tabs).setVisibility(View.INVISIBLE);
        }


        // Without the "record audio" permission this app is useless...
        if (ContextCompat.checkSelfPermission(this, Permission.REQUEST_RECORD_AUDIO_PERMISSION.getName())
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Permission.REQUEST_RECORD_AUDIO_PERMISSION, new PermissionCallback() {
                @Override
                public void permissionGranted() {
                    // Nothing to do
                }

                @Override
                public void permissionDenied() {
                    // Have to exit
                    Log.e(TAG, "User denied record audio permission. Can't continue");
                    finish();
                }
            });
        }

        // We use the music stream, so make sure the user can adjust the volume for us correctly
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        tabLayout = findViewById(R.id.tabs);

        // Set up the ViewPager with the sections adapter.
        NoSwipeIfActiveViewPager viewPager = findViewById(R.id.container);
        viewPager.setTabLayout(tabLayout);
        viewPager.setAdapter(sectionsPagerAdapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        if (StreamSoundService.getConnectionMaker() != null) {
            tabLayout.getTabAt(1).select();
        }

    }

    /* package */ void requestPermission(final Permission permission, @NonNull final PermissionCallback callback) {
        // 1) Show a dialog telling the user what this is
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(permission.message)
                .setTitle(getString(R.string.app_name) + " needs your permission");

        DialogInterface.OnClickListener okButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 2) Ask for the permission and set the callback for it
                permissionCallbackMap.put(permission.id, callback);
                ActivityCompat.requestPermissions(TabbedMain.this, new String[] {permission.name}, permission.id);

            }
        };
        builder.setPositiveButton("OK", okButtonListener);
        builder.create().show();
    }

    /**
     * Switch to the listen tab
     */
    /* package */ void showListenTab() {
        tabLayout = findViewById(R.id.tabs);
        tabLayout.getTabAt(0).select();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermissionCallback callback = permissionCallbackMap.get(requestCode);
        if ( callback == null) {
            return;
        }
        permissionCallbackMap.delete(requestCode);

        switch ( grantResults[0]) {
            case PackageManager.PERMISSION_GRANTED:
                callback.permissionGranted();
                break;
            case PackageManager.PERMISSION_DENIED:
                callback.permissionDenied();
            default:
                Log.e(TAG, "Unexpected permission response for " + requestCode + ": " + grantResults[0]);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tabbed_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_tabbed_main, container, false);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    /*package*/ class SectionsPagerAdapter extends FragmentPagerAdapter {

        /*package*/ public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ListenFragment();
                case 1:
                    return new SpeakFragment();
                default:
                    throw new RuntimeException("Not a valid fragment: " + position);

            }
        }

        @Override
        public int getCount() {
            return 2;
        }


    }
}
