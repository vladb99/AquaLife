package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.security.PublicKey;

public class KeyExchangeMessage implements Serializable {
    private PublicKey publicKey;
    private boolean respond;

    public KeyExchangeMessage(PublicKey publicKey, boolean respond) {
        this.publicKey = publicKey;
        this.respond = respond;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public boolean getRespond() {
        return respond;
    }
}
