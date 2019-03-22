package peterson.ttu.edu.backupaids.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;

import peterson.ttu.edu.backupaids.service.BaseService;

/**
 * Stops a touch/swipe action if a service is running.
 */
public class NoSwipeIfActiveViewPager extends ViewPager {
    private TabLayout tabLayout;
    private int lastSelectedTabPosition;
    private boolean stoppingASwitch = false;

    public NoSwipeIfActiveViewPager(Context context) {
        super(context);
    }

    public NoSwipeIfActiveViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTabLayout(TabLayout tabLayout) {
        this.tabLayout = tabLayout;
    }

    @Override
    public void setCurrentItem(int item) {
        if ( stoppingASwitch) {
            // This is a switch BACK, so let it proceed
            super.setCurrentItem(item);
            stoppingASwitch = false;
        } else {
            // Not stopping a switch. See if this is allowed (i.e. if we're running a service)
            if ( canSwitchTabs()) {
                super.setCurrentItem(item);
            } else {
                // Need to reverse this
                stoppingASwitch = true;
                tabLayout.getTabAt((item + 1) % tabLayout.getTabCount()).select();
                stoppingASwitch = false;
            }
        }
    }

    /**
     * Can we switch tabs (i.e. are there any services running...)
     * @return
     */
    private boolean canSwitchTabs() {
        if ( BaseService.isAnyServiceRunning()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
            alertDialogBuilder.setTitle("Stop recording/playback?");
            alertDialogBuilder.setMessage("Cannot switch tabs unless you stop playback. Do you wish to stop playback?");
            alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    BaseService.stopAllServices();
                    // Right now this just selects the next tab. If there are ever more than two tabs this won't work...
                    int currentPosition = tabLayout.getSelectedTabPosition();
                    int newPosition = (currentPosition + 1) % tabLayout.getTabCount();
                    stoppingASwitch = true;
                    tabLayout.getTabAt(newPosition).select();
                    stoppingASwitch = false;
                }
            });
            alertDialogBuilder.setNegativeButton("No", null);
            alertDialogBuilder.create().show();
            return false;
        }
        return true;
    }
}
