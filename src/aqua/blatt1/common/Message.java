package aqua.blatt1.common;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Message {
    private final Serializable payload;
    private final AquaClient sender;

    public Message(Serializable payload, AquaClient sender) {
        this.payload = payload;
        this.sender = sender;
    }

    public Serializable getPayload() {
        return this.payload;
    }

    public AquaClient getSender() {
        return this.sender;
    }
}
