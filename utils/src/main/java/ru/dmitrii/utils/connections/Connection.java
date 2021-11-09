package ru.dmitrii.utils.connections;

import ru.dmitrii.utils.models.Message;
import ru.dmitrii.utils.printers.ConsolePrinter;
import ru.dmitrii.utils.printers.PrintMessage;

import javax.crypto.*;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class Connection implements Closeable {
    private final Socket socket;
    private final PrintMessage printMessage;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private Cipher cipherOut;
    private Cipher cipherIn;
    private SecretKey key;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        printMessage = new ConsolePrinter();
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Метод отправки ключа
     */
    public synchronized void sendKey(Message message) {
        if (key == null) {
            try {
                key = Encryption.genKey();
                initCipher();
                String data = Encryption.convertSecretKeyToString(key);
                message.setData(Encryption.encryptionString(data, 4));
                synchronized (out) {
                    out.writeObject(message);
                }
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
            // создам объект и защищаем его конфиденциальность
            SealedObject sealedObject = new SealedObject(message, cipherOut);
            synchronized (out) {
                out.writeObject(sealedObject);
            }
        } catch (IOException | IllegalBlockSizeException e) {
            printMessage.writeMessage("Ошибка отправки сообщения " + e.getMessage());
        }
    }

    /**
     * Метод получения зашифрованого Message
     *
     * @return Message
     */
    public Message receive() throws IOException {
        Message message = null;
        try {
            if (key == null) {
                synchronized (in) {
                    message = (Message) in.readObject();
                    String data = message.getData();
                    key = Encryption.convertStringToSecretKey(Encryption.decryptString(data,4));
                }
                initCipher();
            } else {
                synchronized (in) {
                    // получаем защищенный объект
                    SealedObject sealedObject = (SealedObject) in.readObject();
                    message = (Message) sealedObject.getObject(cipherIn);
                }
            }
        } catch (ClassNotFoundException | IllegalBlockSizeException | BadPaddingException e) {
            printMessage.writeMessage("Ошибка получения Message " + e.getMessage());
        }
        return message;
    }

    /**
     * Инициализация Ciphers
     */
    private void initCipher() {
        try {
            cipherOut = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipherOut.init(Cipher.ENCRYPT_MODE, key);
            cipherIn = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipherIn.init(Cipher.DECRYPT_MODE, key);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            printMessage.writeMessage("Ошибка инициализации Cipher " + e.getMessage());
        }
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


}
