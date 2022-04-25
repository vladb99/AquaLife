package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class LocationUpdate implements Serializable {
    private String fishId;

    public LocationUpdate(String fishId) {
        this.fishId = fishId;
    }

    public String getFishId() {
        return fishId;
    }
}
