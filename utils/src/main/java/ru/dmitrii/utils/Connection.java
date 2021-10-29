package ru.dmitrii.utils;

import ru.dmitrii.utils.models.Message;
import ru.dmitrii.utils.printers.ConsolePrinter;
import ru.dmitrii.utils.printers.PrintMessage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


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
            key = new SecretKeySpec(new byte[] {'0','2','3','4','5','6','7','8','9','1','2','3','4','5','6','7'},"AES" );
            System.out.println(Arrays.toString(key.getEncoded()));
            cipherOut = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherOut.init(Cipher.ENCRYPT_MODE, key);
            cipherIn = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherIn.init(Cipher.DECRYPT_MODE, key);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(socket.getOutputStream(), cipherOut);
            CipherInputStream cipherInputStream = new CipherInputStream(socket.getInputStream(), cipherIn);
            out = new ObjectOutputStream(new BufferedOutputStream(cipherOutputStream));
            in = new ObjectInputStream(new BufferedInputStream(cipherInputStream));


        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            printMessage.writeMessage("Ошибка создания Connection " + e.getMessage());

        }
    }

    /**
     * Метод отправки Message
     *
     * @param message Message
     */
    public synchronized void send(Message message) {
            try {
                cipherOut.init(Cipher.ENCRYPT_MODE, key);
                SealedObject sealedObject = new SealedObject(message, cipherOut);
                out.writeObject(sealedObject);
            } catch (IOException | IllegalBlockSizeException | InvalidKeyException e) {
                printMessage.writeMessage("Ошибка отправки сообщения " + e.getMessage());
            }
    }

    /**
     * Метод получения Message
     *
     * @return Message
     */
    public synchronized Message receive() throws IOException {
            Message message = null;
            try {
                cipherIn.init(Cipher.DECRYPT_MODE, key);
                System.out.println("Получение ");
                SealedObject sealedObject = (SealedObject) in.readObject();
                message = (Message) sealedObject.getObject(cipherIn);
                ;
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
}
