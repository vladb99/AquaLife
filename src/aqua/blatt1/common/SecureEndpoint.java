package aqua.blatt1.common;

import messaging.Endpoint;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.net.InetSocketAddress;

public class SecureEndpoint extends Endpoint {
    private Endpoint endpoint;
    private SecretKeySpec keySpec;
    private Cipher decodeCipher;
    private Cipher encodeCipher;

    public SecureEndpoint(int port) {
        this.endpoint = new Endpoint(port);
        this.keySpec = new SecretKeySpec(new String("CAFEBABECAFEBABE").getBytes(),"AES");

        try {
            this.decodeCipher = Cipher.getInstance("AES");
            decodeCipher.init(Cipher.DECRYPT_MODE, keySpec);

            this.encodeCipher = Cipher.getInstance("AES");
            encodeCipher.init(Cipher.ENCRYPT_MODE, keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {

            endpoint.send(receiver, newPayload);
        } catch (Exception var7) {
            throw new RuntimeException(var7);
        }
    }
}
