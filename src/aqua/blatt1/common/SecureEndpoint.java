package aqua.blatt1.common;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint {
    private Endpoint endpoint;
    private SecretKeySpec keySpec;
    private Cipher decodeCipher;
    private Cipher encodeCipher;

    public SecureEndpoint() {
        this.endpoint = new Endpoint();
    }

    public SecureEndpoint(int port) {
        this.endpoint = new Endpoint(port);
    }

    {
        this.keySpec = new SecretKeySpec(new String("CAFEBABECAFEBABE").getBytes(), "AES");

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
        System.out.println("SEND");
        try {
            endpoint.send(receiver, new SealedObject(payload, encodeCipher));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
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
        SealedObject sealedObject;
        try {
            //sealedObject = new SealedObject(message.getPayload(), decodeCipher);
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




    public static SealedObject encryptObject(String algorithm, Serializable object,
                                             SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IOException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        SealedObject sealedObject = new SealedObject(object, cipher);
        return sealedObject;
    }

    public static Serializable decryptObject(String algorithm, SealedObject sealedObject,
                                             SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            ClassNotFoundException, BadPaddingException, IllegalBlockSizeException,
            IOException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        Serializable unsealObject = (Serializable) sealedObject.getObject(cipher);
        return unsealObject;
    }

}
