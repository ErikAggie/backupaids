package peterson.ttu.edu.backupaids.network;

public class ReadyConnectionMaker {

    private static ConnectionMaker readyConnectionMaker = null;

    public static void setReadyConnectionMaker(ConnectionMaker connectionMaker) {
        readyConnectionMaker = connectionMaker;
    }

    public static ConnectionMaker getReadyConnectionMaker() {
        return readyConnectionMaker;
    }
}
