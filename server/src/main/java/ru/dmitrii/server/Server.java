package ru.dmitrii.server;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.dmitrii.server.bot.BotClient;
import ru.dmitrii.jdbc.DataConfiguration;
import ru.dmitrii.jdbc.dao.MessageDAO;
import ru.dmitrii.jdbc.dao.UserDAO;
import ru.dmitrii.utils.connections.Connection;
import ru.dmitrii.utils.UtilsConfiguration;
import ru.dmitrii.utils.models.Message;
import ru.dmitrii.utils.models.MessageImpl;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final PrintMessage PRINT_MESSAGE = new ConsolePrinter();
    private static final List<ThreadMessages> HANDLER_LIST = new ArrayList<>();
    protected final Map<String, Connection> connectionsMap = new ConcurrentHashMap<>();
    private final User SERVER_USER = new User(2, "server", "server");
    private UserDAO userDAO;
    private MessageDAO messageDAO;
    private BotClient botClient;

    public static void main(String[] args) {
        Server server = new Server();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DataConfiguration.class, UtilsConfiguration.class, BotClient.class);
        context.refresh();
        server.userDAO = context.getBean(UserDAO.class);
        server.messageDAO = context.getBean(MessageDAO.class);
        server.botClient = context.getBean(BotClient.class, server);
        PRINT_MESSAGE.writeMessage("Введите порт на котором будет работать чат-сервер:");
        try (ServerSocket serverSocket = new ServerSocket(PRINT_MESSAGE.readInt())) {
            PRINT_MESSAGE.writeMessage("Сервер запущен");
            //Поток ожидающий входящее соединение
            Thread threadSocket = server.getThreadSockets(serverSocket);
            threadSocket.start();
            while (true) {
                PRINT_MESSAGE.writeMessage("Для остановки сервера введите - stop");
                if (!server.botClient.isRun()) PRINT_MESSAGE.writeMessage("Для запуска бота введите - bot");
                String string = PRINT_MESSAGE.readString();
                if (string.equalsIgnoreCase("bot")) {
                    server.botClient.start();
                    PRINT_MESSAGE.writeMessage("Чат-бот запущен " + !server.botClient.isRun());
                }
                if (string.equalsIgnoreCase("stop")) {
                    server.stopServer(serverSocket, threadSocket);
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
    private Thread getThreadSockets(ServerSocket serverSocket) {
        return new Thread(() -> {
            // accept ждёт пока кто-либо не захочет подсоединится к нему. возврашает Socket
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ThreadMessages handlerSocket = new ThreadMessages(serverSocket.accept());
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
    private void stopServer(ServerSocket serverSocket, Thread threadSocket) throws IOException {
        botClient.stopWork();
        HANDLER_LIST.forEach(Thread::interrupt);
        connectionsMap.forEach((k, v) -> {
            v.send(new MessageImpl(MessageType.SERVER_DISCONNECT, "Выключение сервера", SERVER_USER));
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
    public void sendBroadcastMessage(Message message) {
        messageDAO.save(message);
        for (String clientName : connectionsMap.keySet()) {
            connectionsMap.get(clientName).send(message);
        }
    }

    /**
     * Метод транслирующий сообщение клиенту
     *
     * @param message Message
     */
    public void sendUserMessage(Message message, String to) {
        messageDAO.save(message);
        Connection connection = connectionsMap.get(to);
        if (connection != null) {
        connection.send(message);
        }
    }

    /**
     * Получить пользователей онлайн
     * @return Set
     */
    public Set<String> getOnline() {
        return connectionsMap.keySet();
    }

    /**
     * Внутренний класс-поток работающий с сообщениями
     */
    private class ThreadMessages extends Thread {
        private final Socket socket;

        public ThreadMessages(Socket socket) {
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
            String error = "Превышено допустимое количество попыток попыток ввода пароля";
            // Отправляем зашифрованнный ключ
            connection.sendKey(new MessageImpl(MessageType.CONNECT, "", SERVER_USER));
            int count = 0;
            while (!accepted) {
                //Проверяем количество ввода пароля
                if (count >= 3) {
                    connection.send(new MessageImpl(MessageType.SERVER_DISCONNECT, error, SERVER_USER));
                    connection.close();
                    break;
                }
                // Запрашиваем пароль
                connection.send(new MessageImpl(MessageType.NAME_REQUEST, wrong, SERVER_USER));
                Message message = connection.receive();
                if (message.getType() == MessageType.USER_NAME) {
                    String[] split = message.getData().split(" :: ");
                    name = split[0].trim();
                    password = split[1].trim();
                    message.setData(name);
                    messageDAO.save(message);
                    if (name.length() > 2 && name.length() < 24 && password.length() > 3) {
                        if (connectionsMap.get(name) != null) {
                            wrong = "Пользователь уже подключён";
                            continue;
                        }
                        // Проверяем что пользователя нет
                        if (userDAO.checkNoUser(name)) {
                            connectionsMap.putIfAbsent(name, connection);
                            int index = userDAO.save(new User(name, password));
                            connection.send(new MessageImpl(MessageType.NAME_ACCEPTED, String.valueOf(index), SERVER_USER));
                            accepted = true;
                            continue;
                        }
                        //Проверяем совпадает ли пользователь и пароль
                        else if (userDAO.checkUser(name, password)) {
                            connectionsMap.putIfAbsent(name, connection);
                            int index = userDAO.indexUser(name);
                            connection.send(new MessageImpl(MessageType.NAME_ACCEPTED, String.valueOf(index), SERVER_USER));
                            accepted = true;
                        }
                        wrong = "Не правильный пароль пользователя";
                        count++;
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
            for (String clientName : connectionsMap.keySet()) {
                if (!clientName.equals(userName)) {
                    connection.send(new MessageImpl(MessageType.USER_ADDED, clientName, SERVER_USER));
                }
            }
        }

        /**
         * Отправить подключившемуся пользователю сообщения за последние два часа
         *
         * @param connection Connection
         */
        private void sendRecentMessages(Connection connection) {
            List<Message> messages = messageDAO.showMessageTime();
            messages.forEach(n -> n.setData(getStringMessage(n)));
            messages.forEach(connection::send);
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
                if (message.getType() == MessageType.TEXT) {
                    if (message.getData().toLowerCase().startsWith("bot")) {
                        botClient.handleMessage(message);
                    } else {
                        String messageText = getStringMessage(message);
                        PRINT_MESSAGE.writeMessage(messageText);
                        message.setData(messageText);
                        sendBroadcastMessage(message);
                    }
                } else PRINT_MESSAGE.writeMessage(
                        String.format("Ошибка! Недопустимый тип сообщения (MessageType.%s) от клиента: %s",
                                message.getType().toString(), message.getAuthor()));

            }

        }

        /**
         * Преобразование сообщения
         *
         * @param message Message
         * @return String
         */
        private String getStringMessage(Message message) {
            return String.format("%s (%s) : %s", message.getAuthor().getName(), message.getDateTime()
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT,
                            FormatStyle.SHORT)), message.getData());
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
                Message messageAdd = new MessageImpl(MessageType.USER_ADDED, clientName, SERVER_USER);
                sendBroadcastMessage(messageAdd);
                PRINT_MESSAGE.writeMessage(String.format("%s присоединился к серверу", clientName));
                notifyAddUser(connection, clientName);
                sendRecentMessages(connection);
                if (botClient.isRun()) botClient.clientsMessage(clientName);
                serverMessageLoop(connection);
            } catch (IOException e) {
                PRINT_MESSAGE.writeMessage(e.getMessage());
            }
            if (clientName != null) {
                connectionsMap.remove(clientName);
                sendBroadcastMessage(new MessageImpl(MessageType.USER_REMOVED, clientName, SERVER_USER));
            }
            PRINT_MESSAGE.writeMessage(String.format("Соединение с удаленным адресом (%s) закрыто.", socket.getRemoteSocketAddress()));
        }
    }
}
