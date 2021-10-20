package ru.dmitrii.client;

import utils.Connection;
import utils.models.Message;
import utils.models.MessageType;
import utils.models.User;
import utils.printers.ConsolePrinter;
import utils.printers.PrintMessage;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;
    private static final PrintMessage PRINT_MESSAGE = new ConsolePrinter();
    private User currentUser;
    private static final User unknown = new User(2,"unknown", "");

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    /**
     * Получение адресса сервера
     * @return String
     */
    protected String getServerAddress() {
        PRINT_MESSAGE.writeMessage("Введите адресс сервера:");
        return PRINT_MESSAGE.readString();
    }

    /**
     * Получение порта сервера
     * @return int
     */
    protected int getServerPort() {
        PRINT_MESSAGE.writeMessage("Введите порт сервера:");
        return PRINT_MESSAGE.readInt();
    }

    /**
     * Получение имени пользователя
     * @return String
     */
    protected String getUserName() {
        PRINT_MESSAGE.writeMessage("Введите имя пользователя:");
        return PRINT_MESSAGE.readString();
    }

    /**
     * Получение пароля
     * @return String
     */
    protected String getPassword() {
        PRINT_MESSAGE.writeMessage("Введите пароль:");
        return PRINT_MESSAGE.readString();
    }

    /**
     * Получить поток обработки сообщений
     * @return SocketThread
     */
    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    /**
     * Отправка сообщения TEXT
     * @param text String
     */
    protected void sendTextMessage(String text) {
            connection.send(new Message(MessageType.TEXT, text, currentUser));
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                // ожидаем окончания соединения с сервером
                this.wait();
            } catch (InterruptedException e) {
                PRINT_MESSAGE.writeMessage("Произошла ошибка ожидания");
                return;
            }
        }
        if (clientConnected) PRINT_MESSAGE.writeMessage("Соединение установлено.\n" +
                "Для выхода наберите команду 'exit'.");
        else PRINT_MESSAGE.writeMessage("Произошла ошибка во время работы клиента.");
        while (clientConnected) {
            String text = PRINT_MESSAGE.readString();
            if (text.equals("exit")) break;
            sendTextMessage(text);
        }
        PRINT_MESSAGE.writeMessage("Выключение");
    }

    /**
     * Внутренний класс-поток работающий с сообщениями
     */
    public class SocketThread extends Thread {
        @Override
        public void run() {
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMessageLoop();
            } catch (IOException e) {
                notifyStatusConnection(false);
            }
        }

        /**
         * Вывод полученного сообщения
         * @param message String
         */
        protected void processIncomingMessage(String message) {
            PRINT_MESSAGE.writeMessage(message);
        }

        /**
         * Информирование об присоединении пользователя
         * @param userName String
         */
        protected void informAddUser(String userName) {
            PRINT_MESSAGE.writeMessage(userName + " присоединился к чату");
        }

        /**
         * Информирование об отсоединении пользователя
         * @param userName String
         */
        protected void informDeleteUser(String userName) {
            PRINT_MESSAGE.writeMessage(userName + " покинул чат");
        }

        /**
         * Изменить статус соединения
         * @param clientConnected boolean
         */
        protected void notifyStatusConnection(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        /**
         * Проверка имени и подключение к серверу
         * @throws IOException IOException
         */
        protected void clientHandshake() throws IOException {
            String clientName = "";
            String password = "";
            while (!clientConnected) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    PRINT_MESSAGE.writeMessage(message.getData());
                    clientName = getUserName();
                    password = getPassword();
                    connection.send(new Message(MessageType.USER_NAME, clientName + " :: " + password, unknown));
                } else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    int index = Integer.parseInt(message.getData());
                    PRINT_MESSAGE.writeMessage("Получен индекс " + index);
                    currentUser = new User(index, clientName, password);
                    notifyStatusConnection(true);
                } else throw new IOException("Unexpected MessageType");
            }
        }

        /**
         * Цикл получения и обработки сообщений
         * @throws IOException IOException
         */
        protected void clientMessageLoop() throws IOException{
            while (clientConnected) {
                Message message = connection.receive();
                switch (message.getType()) {
                    case TEXT:
                        processIncomingMessage(message.getData());
                        break;
                    case USER_ADDED:
                        informAddUser(message.getData());
                        break;
                    case USER_REMOVED:
                        informDeleteUser(message.getData());
                        break;
                    case SERVER_DISCONNECT: {
                        PRINT_MESSAGE.writeMessage(message.getData());
                        notifyStatusConnection(false);
                        break;
                    }
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }
    }
}