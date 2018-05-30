package peterson.ttu.edu.backupaids.activities;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import peterson.ttu.edu.backupaids.service.BaseService;

/**
 * Stops a touch/swipe action if a service is running.
 */
public class NoSwipeIfActiveViewPager extends ViewPager {
    public NoSwipeIfActiveViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (BaseService.isAnyServiceRunning()) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if ( BaseService.isAnyServiceRunning()) {
            return false;
        }
        return super.onTouchEvent(ev);
    }
}
