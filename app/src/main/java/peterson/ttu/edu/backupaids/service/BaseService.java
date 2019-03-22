package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class that keeps track of all running services
 */
public abstract class BaseService extends IntentService {

    private static final CopyOnWriteArrayList<BaseService> ALL_SERVICES = new CopyOnWriteArrayList<>();

    public BaseService(String name) {
        super(name);
    }

    protected void registerThisService() {
        ALL_SERVICES.add(this);
    }

    protected void unregisterThisService() {
        ALL_SERVICES.remove(this);
    }

    protected abstract boolean isRunning();

    protected abstract void stopNow();

    public static boolean isAnyServiceRunning() {
        for ( BaseService service : ALL_SERVICES) {
            if ( service.isRunning()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stop all running services
     * @return
     */
    public static void stopAllServices() {
        for ( BaseService service : ALL_SERVICES) {
            if ( service.isRunning()) {
                // Use this to make sure that the service won't report itself as running anymore
                service.stopNow();
            }
        }
    }
}
