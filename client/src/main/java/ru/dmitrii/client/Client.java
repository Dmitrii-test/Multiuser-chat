package ru.dmitrii.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.dmitrii.utils.connections.Connection;
import ru.dmitrii.utils.UtilsConfiguration;
import ru.dmitrii.utils.models.*;
import ru.dmitrii.utils.printers.PrintMessage;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    protected volatile boolean clientConnected = false;
    protected final PrintMessage printMessage;
    protected User currentUser;
    protected static final User UNKNOWN = new UserImpl(2, "unknown", "");
    private final Logger logger = LoggerFactory.getLogger(Client.class);

    public Client() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(UtilsConfiguration.class);
        context.refresh();
        printMessage = context.getBean(PrintMessage.class);
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    /**
     * Получение адресса сервера
     *
     * @return String
     */
    protected String getServerAddress() {
        printMessage.writeMessage("Введите адресс сервера:");
        return printMessage.readString();
    }

    /**
     * Получение порта сервера
     *
     * @return int
     */
    protected int getServerPort() {
        printMessage.writeMessage("Введите порт сервера:");
        return printMessage.readInt();
    }

    /**
     * Выбор авторизации
     *
     * @return MessageType
     */
    protected MessageType getAuthorization() {
        while (true) {
            printMessage.writeMessage("Выберите: 1 - зарегистрироваться, 2 - аутентифицироваться");
            int i = printMessage.readInt();
            if (i == 1) return MessageType.USER_SIGNUP;
            if (i == 2) return MessageType.USER_LOGIN;
            else printMessage.writeMessage("Не правильный выбор");
        }
    }

    /**
     * Получение имени пользователя
     *
     * @return String
     */
    protected String getUserName() {
        printMessage.writeMessage("Введите имя пользователя:");
        return printMessage.readString();
    }

    /**
     * Получение пароля
     *
     * @return String
     */
    protected String getPassword() {
        printMessage.writeMessage("Введите пароль:");
        return printMessage.readString();
    }

    /**
     * Получить поток обработки сообщений
     *
     * @return SocketThread
     */
    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    /**
     * Отправка сообщения TEXT
     *
     * @param text String
     */
    public void sendTextMessage(String text) {
        connection.send(new MessageImpl(MessageType.TEXT, text, currentUser));
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
                printMessage.writeMessage("Произошла ошибка ожидания");
                logger.error("Произошла ошибка ожидания" + e.getMessage());
                return;
            }
        }
        if (clientConnected) {
            printMessage.writeMessage("Соединение установлено.\nДля выхода наберите команду 'exit'.");
        } else printMessage.writeMessage("Произошла ошибка во время плдключения.");
        while (clientConnected) {
            String text = printMessage.readString();
            if (text.equals("exit")) break;
            sendTextMessage(text);
        }
        printMessage.writeMessage("Выключение");
        logger.info("Клиент выключен.");
    }

    /**
     * Внутренний класс-поток работающий с сообщениями
     */
    public class SocketThread extends Thread {
        @Override
        public void run() {
            try {
                logger.info("Клиент запущен");
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                logger.info("Установленно соединение с сервером {}", connection.getRemoteSocketAddress());
                clientHandshake();
                clientMessageLoop();
            } catch (IOException e) {
                printMessage.writeMessage("Ошибка установления связи " + e.getMessage());
                logger.error("Ошибка установления связи " + e.getMessage());
                notifyStatusConnection(false);
            }
        }

        /**
         * Вывод полученного сообщения
         *
         * @param message String
         */
        protected void processIncomingMessage(String message) {
            printMessage.writeMessage(message);
        }

        /**
         * Информирование об присоединении пользователя
         *
         * @param userName String
         */
        protected void informAddUser(String userName) {
            printMessage.writeMessage(userName + " присоединился к чату");
        }

        /**
         * Информирование об отсоединении пользователя
         *
         * @param userName String
         */
        protected void informDeleteUser(String userName) {
            printMessage.writeMessage(userName + " покинул чат");
        }

        /**
         * Изменить статус соединения
         *
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
         *
         * @throws IOException IOException
         */
        protected void clientHandshake() throws IOException {
            String clientName = "";
            String password = "";
            while (!clientConnected) {
                Message message = connection.receive();
                switch (message.getType()) {
                    // Получение зашифрованного ключа
                    case CONNECT:
                        break;
                    // Дисконект при 3-х не правильных паролях
                    case SERVER_DISCONNECT:
                        printMessage.writeMessage(message.getData());
                        logger.error("Исчерпаны попытки подключения к пользователю {}", clientName);
                        notifyStatusConnection(false);
                        break;
                    // Ввод имени и пароля
                    case NAME_REQUEST:
                        printMessage.writeMessage(message.getData());
                        MessageType type = getAuthorization();
                        clientName = getUserName();
                        password = getPassword();
                        connection.send(new MessageImpl(type,
                                clientName + " :: " + password, UNKNOWN));
                        break;
                    // Имя подтверждено
                    case NAME_ACCEPTED:
                        int index = Integer.parseInt(message.getData());
                        currentUser = new UserImpl(index, clientName, password);
                        logger.info("Доступ к {} подтверждён", clientName);
                        notifyStatusConnection(true);
                        break;
                    default:
                        logger.error("Ошибка типа сообщения {} при получении имени", message.getType());
                        throw new IOException("Ошибка типа сообщения при авторизации " + message.getType());
                }
            }
        }

        /**
         * Цикл получения и обработки сообщений
         *
         * @throws IOException IOException
         */
        protected void clientMessageLoop() throws IOException {
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
                        printMessage.writeMessage(message.getData());
                        notifyStatusConnection(false);
                        break;
                    }
                    default:
                        logger.error("Ошибка типа сообщения {} в цикле работы", message.getType());
                        throw new IOException("Ошибка типа сообщения в чате");
                }
            }
        }
    }
}