package peterson.ttu.edu.backupaids.activities.headsetSetup;

/**
 * Holds the adjustments for a particular profile;
 */

public class HeadsetSettings {
    private String mName;
    private int mBaseVolume = -1; // A value to indicate it's not initialized
    private int m125Adjustment;
    private int m250Adjustment;
    private int m500Adjustment;
    private int m1000Adjustment;
    private int m2000Adjustment;
    private int m3000Adjustment;
    private int m4000Adjustment;
    private int m8000Adjustment;

    public boolean isPopulated() {
        return mBaseVolume >= 0;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public int getBaseVolume() {
        return mBaseVolume;
    }

    public void setBaseVolume(int mBaseVolume) {
        this.mBaseVolume = mBaseVolume;
    }

    public int get125Adjustment() {
        return m125Adjustment;
    }

    public void set125Adjustment(int m125Adjustment) {
        this.m125Adjustment = m125Adjustment;
    }

    public int get250Adjustment() {
        return m250Adjustment;
    }

    public void set250Adjustment(int m250Adjustment) {
        this.m250Adjustment = m250Adjustment;
    }

    public int get500Adjustment() {
        return m500Adjustment;
    }

    public void set500Adjustment(int m500Adjustment) {
        this.m500Adjustment = m500Adjustment;
    }

    public int get1000Adjustment() {
        return m1000Adjustment;
    }

    public void set1000Adjustment(int m1000Adjustment) {
        this.m1000Adjustment = m1000Adjustment;
    }

    public int get2000Adjustment() {
        return m2000Adjustment;
    }

    public void set2000Adjustment(int m2000Adjustment) {
        this.m2000Adjustment = m2000Adjustment;
    }

    public int get3000Adjustment() {
        return m3000Adjustment;
    }

    public void set3000Adjustment(int m3000Adjustment) {
        this.m3000Adjustment = m3000Adjustment;
    }

    public int get4000Adjustment() {
        return m4000Adjustment;
    }

    public void set4000Adjustment(int m4000Adjustment) {
        this.m4000Adjustment = m4000Adjustment;
    }

    public int get8000Adjustment() {
        return m8000Adjustment;
    }

    public void set8000Adjustment(int m8000Adjustment) {
        this.m8000Adjustment = m8000Adjustment;
    }
}
