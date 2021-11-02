package ru.dmitrii.server;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.dmitrii.jdbc.DataConfiguration;
import ru.dmitrii.jdbc.dao.MessageDAO;
import ru.dmitrii.jdbc.dao.UserDAO;
import ru.dmitrii.utils.Connection;
import ru.dmitrii.utils.UtilsConfiguration;
import ru.dmitrii.utils.models.Message;
import ru.dmitrii.utils.models.MessageType;
import ru.dmitrii.utils.models.User;
import ru.dmitrii.utils.printers.ConsolePrinter;
import ru.dmitrii.utils.printers.PrintMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static private final Map<String, Connection> CONNECTION_MAP = new ConcurrentHashMap<>();
    static private final PrintMessage PRINT_MESSAGE = new ConsolePrinter();
    static private final List<Handler> HANDLER_LIST = new ArrayList<>();
    static private final User SERVER_USER = new User(2, "server", "server");;
    static private UserDAO userDAO;
    static private MessageDAO messageDAO;

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DataConfiguration.class, UtilsConfiguration.class);
        context.refresh();
        userDAO = context.getBean(UserDAO.class);
        messageDAO = context.getBean(MessageDAO.class);
        PRINT_MESSAGE.writeMessage("Введите порт на котором будет работать чат-сервер:");
        try (ServerSocket serverSocket = new ServerSocket(PRINT_MESSAGE.readInt())) {
            PRINT_MESSAGE.writeMessage("Сервер запущен");
            //Поток ожидающий входящее соединение
            Thread threadSocket = getThreadSockets(serverSocket);
            threadSocket.start();
            while (true) {
                PRINT_MESSAGE.writeMessage("Для остановки сервера введите - stop");
                String string = PRINT_MESSAGE.readString();
                if (string.equalsIgnoreCase("stop")) {
                    stopServer(serverSocket, threadSocket);
                    break;
                }
            }
        } catch (IOException e) {
            PRINT_MESSAGE.writeMessage(e.getMessage());
        }
    }

    /**
     * Создание потока ожидающего подключение
     *
     * @param serverSocket ServerSocket
     * @return Thread
     */
    private static Thread getThreadSockets(ServerSocket serverSocket) {
        return new Thread(() -> {
            // accept ждёт пока кто-либо не захочет подсоединится к нему. возврашает Socket
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Handler handlerSocket = new Handler(serverSocket.accept());
                    HANDLER_LIST.add(handlerSocket);
                    handlerSocket.start();
                } catch (SocketException r) {
                    PRINT_MESSAGE.writeMessage("Остановка сервера");
                } catch (IOException e) {
                    e.printStackTrace();
                    PRINT_MESSAGE.writeMessage("Ошибка ввода вывода " + e.getMessage());
                }

            }
        });
    }

    /**
     * Метод остановки работы сервера
     *
     * @param serverSocket ServerSocket
     * @param threadSocket Thread
     * @throws IOException IOException
     */
    private static void stopServer(ServerSocket serverSocket, Thread threadSocket) throws IOException {
        HANDLER_LIST.forEach(Thread::interrupt);
        CONNECTION_MAP.forEach((k, v) -> {
            v.send(new Message(MessageType.SERVER_DISCONNECT, "Выключение сервера", SERVER_USER));
            v.close();
        });
        serverSocket.close();
        threadSocket.interrupt();
    }

    /**
     * Метод транслирующий сообщение всем клиентам
     *
     * @param message Message
     */
    public static void sendBroadcastMessage(Message message) {
        if (message.getType() != MessageType.TEXT) messageDAO.save(message);
        for (String clientName : CONNECTION_MAP.keySet()) {
            CONNECTION_MAP.get(clientName).send(message);
        }
    }

    /**
     * Внутренний класс-поток работающий с сообщениями
     */
    private static class Handler extends Thread {
        private final Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Метод запроса имени клиента
         *
         * @param connection Connection
         * @return String name
         */
        private String serverHandshake(Connection connection) throws IOException {
            boolean accepted = false;
            String name = "";
            String password = "";
            String wrong = "";
            connection.sendKey(new Message(MessageType.CONNECT, "", SERVER_USER));
            while (!accepted) {
                connection.send(new Message(MessageType.NAME_REQUEST, wrong, SERVER_USER));
                Message message = connection.receive();
                messageDAO.save(message);
                if (message.getType() == MessageType.USER_NAME) {
                    String[] split = message.getData().split(" :: ");
                    name = split[0];
                    password = split[1];
                    if (name.length() > 2 && name.length() < 24 && password.length() > 3) {
                        // Проверяем что пользователя нет
                        if (userDAO.checkNoUser(name) && CONNECTION_MAP.get(name) == null) {
                            CONNECTION_MAP.putIfAbsent(name, connection);
                            int index = userDAO.save(new User(name, password));
                            connection.send(new Message(MessageType.NAME_ACCEPTED, String.valueOf(index), SERVER_USER));
                            accepted = true;
                            continue;
                        }
                        //Проверяем совпадает ли пользователь и пароль
                        else if (userDAO.checkUser(name, password)) {
                            CONNECTION_MAP.putIfAbsent(name, connection);
                            int index = userDAO.indexUser(name);
                            connection.send(new Message(MessageType.NAME_ACCEPTED, String.valueOf(index), SERVER_USER));
                            accepted = true;
                        }
                        wrong = "Не правильный пароль пользователя";
                        continue;
                    }
                    wrong = "Не допустимая длина имени пользователя или пароля(имя пользвателя не меньше 3 символов, " +
                            "пароль не меньше 4 символов)";
                }
            }
            return name;
        }

        /**
         * Уведомить всех о подключении нового клинта
         *
         * @param connection Connection
         * @param userName   String
         */
        private void notifyAddUser(Connection connection, String userName) {
            for (String clientName : CONNECTION_MAP.keySet()) {
                if (!clientName.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, clientName, SERVER_USER));
                }
            }
        }

        /**
         * Цикл приема и обработки сообщений Text
         *
         * @param connection Connection
         * @throws IOException IOException
         */
        private void serverMessageLoop(Connection connection) throws IOException {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = connection.receive();
                messageDAO.save(message);
                User author = message.getAuthor();
                if (message.getType() == MessageType.TEXT) {
                    String messageText = String.format("%s (%s) : %s", author.getName(), message.getDateTime()
                                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)),
                            message.getData());
                    PRINT_MESSAGE.writeMessage(messageText);
                    message.setData(messageText);
                    sendBroadcastMessage(message);
                } else PRINT_MESSAGE.writeMessage(
                        String.format("Ошибка! Недопустимый тип сообщения (MessageType.%s) от клиента: %s",
                                message.getType().toString(), author.getName()));

            }

        }

        @Override
        public void run() {
            PRINT_MESSAGE.writeMessage("Установлено соединение с клиентом с адресом: " +
                    socket.getRemoteSocketAddress());
            Connection connection;
            String clientName = null;
            try {
                connection = new Connection(socket);
                clientName = serverHandshake(connection);
                Message messageAdd = new Message(MessageType.USER_ADDED, clientName, SERVER_USER);
                sendBroadcastMessage(messageAdd);
                PRINT_MESSAGE.writeMessage(String.format("%s присоединился к серверу", clientName));
                notifyAddUser(connection, clientName);
                serverMessageLoop(connection);
            } catch (IOException e) {
                PRINT_MESSAGE.writeMessage(e.getMessage());
            }
            if (clientName != null) {
                CONNECTION_MAP.remove(clientName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, clientName, SERVER_USER));
            }
            PRINT_MESSAGE.writeMessage(String.format("Соединение с удаленным адресом (%s) закрыто.", socket.getRemoteSocketAddress()));
        }

    }
}
