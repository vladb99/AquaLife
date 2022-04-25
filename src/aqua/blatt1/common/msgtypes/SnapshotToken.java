package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class SnapshotToken implements Serializable {
    private int fishies = 0;
    public SnapshotToken(int fishies){
        this.fishies = fishies;
    }

    public int getFishies() {
        return fishies;
    }

    public void setFishies(int fishies) {
        this.fishies = fishies;
    }

    public void increaseFishies(int newFishies){
        this.fishies += newFishies;
    }
}
