package utils;

import utils.models.Message;
import utils.printers.ConsolePrinter;
import utils.printers.PrintMessage;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;

public class Connection implements Closeable {
    final private Socket socket;
    final private ObjectOutputStream out;
    final private ObjectInputStream in;
    final private PrintMessage printMessage;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        printMessage = new ConsolePrinter();
    }

    /**
     * Метод отправки Message
     * @param message Message
     */
    public void send(Message message){
        synchronized (out) {
            try {
                out.writeObject(message);
            } catch (IOException e) {
                printMessage.writeMessage("Ошибка отправки сообщения " + e.getMessage());
            }
        }
    }

    /**
     * Метод получения Message
     * @return Message
     */
    public Message receive() throws IOException {
        synchronized (in) {
            Message message = null;
            try {
                message = (Message) in.readObject();
            } catch (ClassNotFoundException e) {
                printMessage.writeMessage("Ошибка получения Message " + e.getMessage());
            }
            return message;
        }
    }

    /**
     * Метод возвращающий удаленный адрес.
     * @return SocketAddress
     */
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    /**
     * Закрытие всех стримов
     * @throws IOException IOException
     */
    @Override
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}
