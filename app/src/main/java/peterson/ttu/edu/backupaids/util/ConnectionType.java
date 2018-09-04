package peterson.ttu.edu.backupaids.util;

public enum ConnectionType {
    LISTEN(Util.LISTEN_BUDDY_NAME, Util.SPEAK_BUDDY_NAME),
    SPEAK(Util.SPEAK_BUDDY_NAME, Util.LISTEN_BUDDY_NAME);

    private final String myService;
    private final String otherService;

    ConnectionType(String myService, String otherService) {
        this.myService = myService;
        this.otherService = otherService;
    }

    public String getMyService() {
        return myService;
    }

    public String getOtherService() {
        return otherService;
    }
}
