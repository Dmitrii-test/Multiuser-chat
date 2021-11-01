package ru.dmitrii.utils;

import ru.dmitrii.utils.models.Message;
import ru.dmitrii.utils.printers.ConsolePrinter;
import ru.dmitrii.utils.printers.PrintMessage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class Connection implements Closeable {
    private final Socket socket;
    private final PrintMessage printMessage;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Cipher cipherOut;
    private Cipher cipherIn;
    private SecretKey key;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        printMessage = new ConsolePrinter();
        try {
            cipherOut = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipherIn = Cipher.getInstance("AES/ECB/PKCS5Padding");
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            printMessage.writeMessage("Ошибка создания Connection " + e.getMessage());

        }
    }

    /**
     * Метод отправки ключа
     */
    public synchronized void sendKey(Message message) {
        if (key == null) {
            try {
                String genKey = genKey();
                message.setData(genKey);
                out.writeObject(message);
            } catch (IOException | NoSuchAlgorithmException e) {
                printMessage.writeMessage("Ошибка отправки ключа " + e.getMessage());
            }
        }
    }

    /**
     * Метод отправки зашифрованого Message
     *
     * @param message Message
     */
    public void send(Message message) {
        try {
            System.out.println(message);
            cipherOut.init(Cipher.ENCRYPT_MODE, key);
            SealedObject sealedObject = new SealedObject(message, cipherOut);
            out.writeObject(sealedObject);
        } catch (IOException | IllegalBlockSizeException | InvalidKeyException e) {
            printMessage.writeMessage("Ошибка отправки сообщения " + e.getMessage());
        }
    }


    /**
     * Метод получения зашифрованого Message
     *
     * @return Message
     */
    public synchronized Message receive() throws IOException {
        Message message = null;
        try {
            if (key == null) {
                message = (Message) in.readObject();
                System.out.println(message.getData());
                key = convertStringToSecretKey(message.getData());
            }
            else {
                cipherIn.init(Cipher.DECRYPT_MODE, key);
                SealedObject sealedObject = (SealedObject) in.readObject();
                message = (Message) sealedObject.getObject(cipherIn);
            }
        } catch (ClassNotFoundException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            printMessage.writeMessage("Ошибка получения Message " + e.getMessage());
        }
        return message;
    }

    /**
     * Метод возвращающий удаленный адрес.
     *
     * @return SocketAddress
     */
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    /**
     * Закрытие всех стримов
     */
    @Override
    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            printMessage.writeMessage("Ошибка закрытия Connection " + e.getMessage());
        }
    }

    /**
     * Метод генерации ключа
     *
     * @return String
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    private String genKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(192);
        key = keyGenerator.generateKey();
        byte[] rawData = key.getEncoded();
        return Base64.getEncoder().encodeToString(rawData);
    }

    /**
     * Метод получения ключа из строки
     *
     * @param encodedKey String
     * @return SecretKey
     */

    public SecretKey convertStringToSecretKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        return originalKey;
    }

}
