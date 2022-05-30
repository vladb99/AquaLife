package aqua.blatt1.common.msgtypes;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class NeighborUpdate implements Serializable {
    private final AquaClient rightNeighbor;
    private final AquaClient leftNeighbor;

    public NeighborUpdate(AquaClient neighborLeft, AquaClient neighborRight) {
        this.rightNeighbor = neighborRight;
        this.leftNeighbor = neighborLeft;
    }

    public AquaClient getRightNeighbor() {
        return rightNeighbor;
    }

    public AquaClient getLeftNeighbor() {
        return leftNeighbor;
    }
}
