package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private InetSocketAddress inetSocketAddress;
    private String requestId;

    public NameResolutionResponse(InetSocketAddress inetSocketAddress, String requestId) {
        this.inetSocketAddress = inetSocketAddress;
        this.requestId = requestId;
    }

    public InetSocketAddress getAddress() {
        return inetSocketAddress;
    }

    public String getRequestId() {
        return requestId;
    }
}
