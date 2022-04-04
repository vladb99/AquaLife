package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class NeighborUpdate implements Serializable {
    private final InetSocketAddress rightNeighbor;
    private final InetSocketAddress leftNeighbor;

    public NeighborUpdate(InetSocketAddress neighborLeft, InetSocketAddress neighborRight) {
        this.rightNeighbor = neighborRight;
        this.leftNeighbor = neighborLeft;
    }

    public InetSocketAddress getRightNeighbor() {
        return rightNeighbor;
    }

    public InetSocketAddress getLeftNeighbor() {
        return leftNeighbor;
    }
}
