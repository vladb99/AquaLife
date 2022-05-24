package aqua.blatt1.common;

import aqua.blatt1.common.msgtypes.Blank;
import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class SecureEndpoint {
    private Endpoint endpoint;
    private Cipher decodeCipher;

    Map<InetSocketAddress, PublicKey> communicationPartner = new HashMap<>();
    Map<InetSocketAddress, Serializable> toSendLater = new HashMap<>();
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public SecureEndpoint() {
        this.endpoint = new Endpoint();
    }

    public SecureEndpoint(int port) {
        this.endpoint = new Endpoint(port);
    }

    {
        //this.keySpec = new SecretKeySpec(new String("CAFEBABECAFEBABE").getBytes(), "AES");

        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(1024);
            KeyPair pair = keyPairGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();

            this.decodeCipher = Cipher.getInstance("RSA");
            decodeCipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        System.out.println("SEND");
        if (!communicationPartner.containsKey(receiver) ) {
            System.out.println("Sending my key");
            endpoint.send(receiver, new KeyExchangeMessage(publicKey, false));
            toSendLater.put(receiver, payload);
        } else {
            try {
                Cipher encodeCipher = Cipher.getInstance("RSA");
                encodeCipher.init(Cipher.ENCRYPT_MODE, communicationPartner.get(receiver));
                endpoint.send(receiver, new SealedObject(payload, encodeCipher));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    public Message blockingReceive() {
        System.out.println("blockingReceive");
        Message message = endpoint.blockingReceive();
        return decrypt(message);
    }

    public Message nonBlockingReceive() {
        System.out.println("nonBlockingReceive");
        Message message = endpoint.nonBlockingReceive();
        return decrypt(message);
    }

    private Message decrypt(Message message) {
        if (message.getPayload() instanceof KeyExchangeMessage keyExchangeMsg) {
            System.out.println("Got a key");
            communicationPartner.put(message.getSender(), keyExchangeMsg.getPublicKey());
            if (!toSendLater.containsKey(message.getSender())) {
                endpoint.send(message.getSender(), new KeyExchangeMessage(publicKey, false));
                toSendLater.put(message.getSender(), null);
            } else {
                send(message.getSender(), toSendLater.get(message.getSender()));
            }
            return new Message(new Blank(), message.getSender());
        } else {
            try {
                System.out.println(message.getPayload());

                SealedObject so = (SealedObject) message.getPayload();
                Message encryptMessage = new Message((Serializable) so.getObject(decodeCipher), message.getSender());

                System.out.println(encryptMessage.getPayload());

                return encryptMessage;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}