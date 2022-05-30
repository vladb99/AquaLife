package aqua.blatt1.common;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import messaging.Endpoint;
import aqua.blatt1.common.Message;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SecureEndpoint {
    private Endpoint endpoint;
    private Cipher decodeCipher;
    private PrivateKey privKey;
    private PublicKey publicKey;
    Map<InetSocketAddress, PublicKey> communicationPartner = new ConcurrentHashMap<>();

    public SecureEndpoint() {
        this.endpoint = new Endpoint();
    }

    public SecureEndpoint(int port) {
        this.endpoint = new Endpoint(port);
    }

    {
        try {
            //Creating KeyPair generator object
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            //Initializing the KeyPairGenerator
            keyPairGen.initialize(4096);
            //Generate the pair of keys
            KeyPair pair = keyPairGen.generateKeyPair();
            //Getting the private key from the key pair
            privKey = pair.getPrivate();
            //Getting the public key from the key pair
            publicKey = pair.getPublic();

            this.decodeCipher = Cipher.getInstance("RSA");
            decodeCipher.init(Cipher.DECRYPT_MODE, privKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ReentrantLock reentrantLock = new ReentrantLock();

    public void send(InetSocketAddress receiver, Serializable payload) {
        reentrantLock.lock();
        if (!communicationPartner.containsKey(receiver)) {
            endpoint.send(receiver, new KeyExchangeMessage(this.publicKey, true));
            while (!communicationPartner.containsKey(receiver)) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        reentrantLock.unlock();

        try {
            Cipher encodeCipher = Cipher.getInstance("RSA");
            encodeCipher.init(Cipher.ENCRYPT_MODE, communicationPartner.get(receiver));
            new SealedObject(payload, encodeCipher);
            endpoint.send(receiver, new SealedObject(payload, encodeCipher));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

    }

    public Message blockingReceive() {
        Message message = null;
        while (message == null || message.getPayload() instanceof KeyExchangeMessage) {
            message = endpoint.blockingReceive();
            if (message.getPayload() instanceof KeyExchangeMessage kem) {
                communicationPartner.put(message.getSender(), kem.getPublicKey());
                if (kem.getRespond()) {
                    endpoint.send(message.getSender(), new KeyExchangeMessage(this.publicKey, false));
                }
            }
        }
        return decrypt(message);
    }

    public Message nonBlockingReceive() {
        Message message = null;
        while (message == null || message.getPayload() instanceof KeyExchangeMessage) {
            message = endpoint.nonBlockingReceive();

            if(message == null){
                return null;
            }

            if (message.getPayload() instanceof KeyExchangeMessage kem) {
                communicationPartner.put(message.getSender(), kem.getPublicKey());
                if (kem.getRespond()) {
                    endpoint.send(message.getSender(), new KeyExchangeMessage(this.publicKey, false));
                }
            }
        }
        return decrypt(message);
    }

    private Message decrypt(Message message) {
        try {
            SealedObject so = (SealedObject) message.getPayload();
            return new Message((Serializable) so.getObject(decodeCipher), message.getSender());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
