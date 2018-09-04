package peterson.ttu.edu.backupaids.controller;

public interface PeerCallback {
    void approveConnection(String connectionName);
    void denyConnection(String connectionName);
}
