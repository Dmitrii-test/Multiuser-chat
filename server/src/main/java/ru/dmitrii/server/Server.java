package ru.dmitrii.server;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;
import ru.dmitrii.jdbc.DataConfiguration;
import ru.dmitrii.jdbc.dao.MessageDao;
import ru.dmitrii.jdbc.dao.UserDao;
import ru.dmitrii.server.bot.BotClient;
import ru.dmitrii.utils.UtilsConfiguration;
import ru.dmitrii.utils.connections.Connection;
import ru.dmitrii.utils.models.*;
import ru.dmitrii.utils.printers.PrintMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Server {
    protected final Map<String, Connection> connectionsMap = new ConcurrentHashMap<>();
    private final List<ThreadMessages> handlerList = new ArrayList<>();
    private static PrintMessage printMessage;
    private final User SERVER_USER = new UserImpl(2, "server", "server");
    private UserDao userDAO;
    private MessageDao messageDao;
    private BotClient botClient;
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        Server server = new Server();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DataConfiguration.class, UtilsConfiguration.class, BotClient.class);
        context.refresh();
        server.userDAO = context.getBean(UserDao.class);
        server.messageDao = context.getBean(MessageDao.class);
        printMessage = context.getBean(PrintMessage.class);
        server.botClient = context.getBean(BotClient.class, server, server.userDAO, server.messageDao);
//        printMessage.writeMessage("Введите порт на котором будет работать чат-сервер:");
//        try (ServerSocket serverSocket = new ServerSocket(printMessage.readInt())) {
        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]))) {
            printMessage.writeMessage("Сервер запущен");
            logger.info("Сервер запущен на порту {}", serverSocket.getLocalPort());
            //Поток ожидающий входящее соединение
            Thread threadSocket = server.getThreadSockets(serverSocket);
            threadSocket.start();
            while (true) {
                printMessage.writeMessage("Для остановки сервера введите - stop");
                if (!server.botClient.isRun()) printMessage.writeMessage("Для запуска бота введите - bot");
                String string = printMessage.readString();
                if (string.equalsIgnoreCase("bot")) {
                    server.botClient.setDaemon(true);
                    server.botClient.start();
                    printMessage.writeMessage("Чат-бот запущен " + !server.botClient.isRun());
                }
                if (string.equalsIgnoreCase("stop")) {
                    server.stopServer(serverSocket, threadSocket);
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка работы сервера {}", e.getMessage());
            printMessage.writeMessage("Ошибка работы сервера " + e.getMessage());
        }
    }

    /**
     * Создание потока ожидающего подключение
     *
     * @param serverSocket ServerSocket
     * @return Thread
     */
    @NotNull
    @Contract("_ -> new")
    private Thread getThreadSockets(ServerSocket serverSocket) {
        return new Thread(() -> {
            // accept ждёт пока кто-либо не захочет подсоединится к нему. возврашает Socket
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ThreadMessages handlerSocket = new ThreadMessages(serverSocket.accept());
                    handlerList.add(handlerSocket);
                    handlerSocket.start();
                } catch (SocketException r) {
                    printMessage.writeMessage("Остановка сервера");
                    logger.warn("Остановка работы сервера " + r.getMessage());
                } catch (IOException e) {
                    printMessage.writeMessage("Ошибка установки связи" + e.getMessage());
                    logger.error("Ошибка установки связи " + e.getMessage());
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
    private void stopServer(@NotNull ServerSocket serverSocket, @NotNull Thread threadSocket) throws IOException {
        botClient.stopWork();
        handlerList.forEach(Thread::interrupt);
        connectionsMap.forEach((k, v) -> {
            v.send(new MessageImpl(MessageType.SERVER_DISCONNECT, "Выключение сервера", SERVER_USER));
            v.close();
        });
        serverSocket.close();
        threadSocket.interrupt();
        logger.info("Сервер остановлен");
    }

    /**
     * Метод транслирующий сообщение всем клиентам
     *
     * @param message Message
     */
    public void sendBroadcastMessage(Message message) {
        messageDao.save(message);
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
        Connection connection = connectionsMap.get(to);
        if (connection != null) {
            connection.send(message);
        }
    }

    /**
     * Получить Set пользователей онлайн
     *
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
        private String serverHandshake(@NotNull Connection connection) throws IOException {
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
                    logger.warn("Превышено допустимое количество попыток попыток ввода пароля {}",
                            connection.getRemoteSocketAddress());
                    connection.close();
                    break;
                }
                // Запрашиваем пароль
                connection.send(new MessageImpl(MessageType.NAME_REQUEST, wrong, SERVER_USER));
                Message message = connection.receive();
                String[] split = message.getData().split(" :: ");
                name = split[0].trim();
                password = split[1].trim();
                message.setData(name);
                messageDao.save(message);
                if (name.length() < 2 && name.length() > 24 && password.length() < 3) {
                    wrong = "Не допустимая длина имени пользователя или пароля(имя пользвателя не меньше 3 символов, " +
                            "пароль не меньше 4 символов)";
                }
                if (message.getType() == MessageType.USER_SIGNUP) {
                    if (connectionsMap.get(name) != null) {
                        wrong = "Пользователь уже подключён";
                        logger.warn("Пользователь {} уже подключён", name);
                        continue;
                    }
                    // Проверяем, что пользователя нет и сохраняем его
                    if (userDAO.checkNoUser(name)) {
                        connectionsMap.putIfAbsent(name, connection);
                        int index = userDAO.save(new UserImpl(name, password));
                        connection.send(new MessageImpl(MessageType.NAME_ACCEPTED, String.valueOf(index), SERVER_USER));
                        accepted = true;
                        continue;
                    }
                    wrong = "Пользователь уже существует";

                    //Проверяем совпадает ли пользователь и пароль
                    if (userDAO.checkUser(name, password)) {
                        logger.info("Пользователь {} авторизовался", name);
                        connectionsMap.putIfAbsent(name, connection);
                        int index = userDAO.indexUser(name);
                        connection.send(new MessageImpl(MessageType.NAME_ACCEPTED, String.valueOf(index), SERVER_USER));
                        accepted = true;
                    }
                    wrong = "Не правильный пароль пользователя";
                    count++;
                    continue;
                }
                if (message.getType() == MessageType.USER_LOGIN) {
                    if (connectionsMap.get(name) != null) {
                        wrong = "Пользователь уже подключён";
                        logger.warn("Пользователь {} уже подключён", name);
                        continue;
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
            private void notifyAddUser (Connection connection, String userName){
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
            private void sendRecentMessages (@NotNull Connection connection){
                List<Message> messages = messageDao.showMessageTime(2);
                if (messages != null) {
                    messages.forEach(n -> n.setData(getStringMessage(n)));
                    messages.forEach(connection::send);
                }
            }

            /**
             * Цикл приема и обработки сообщений Text
             *
             * @param connection Connection
             * @throws IOException IOException
             */
            private void serverMessageLoop (Connection connection) throws IOException {
                while (!Thread.currentThread().isInterrupted()) {
                    Message message = connection.receive();
                    if (message.getType() == MessageType.TEXT) {
                        if (message.getData().toLowerCase().startsWith("bot")) {
                            botClient.handleMessage(message);
                        } else {
                            String messageText = getStringMessage(message);
                            printMessage.writeMessage(messageText);
                            message.setData(messageText);
                            sendBroadcastMessage(message);
                        }
                    } else {
                        printMessage.writeMessage(
                                String.format("Ошибка! Недопустимый тип сообщения (MessageType.%s) от клиента: %s",
                                        message.getType().toString(), message.getAuthor()));
                        logger.warn("Недопустимый тип сообщения (MessageType.{}) от клиента: {}", message.getType().toString(),
                                message.getAuthor());
                    }
                }
            }

            /**
             * Преобразование сообщения
             *
             * @param message Message
             * @return String
             */
            private String getStringMessage (@NotNull Message message){
                return String.format("%s (%s) : %s", message.getAuthor().getName(), message.getDateTime()
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT,
                                FormatStyle.SHORT)), message.getData());
            }

            @Override
            public void run () {
                printMessage.writeMessage("Установлено соединение с клиентом с адресом: " +
                        socket.getRemoteSocketAddress());
                Connection connection;
                String clientName = null;
                try {
                    connection = new Connection(socket);
                    clientName = serverHandshake(connection);
                    Message messageAdd = new MessageImpl(MessageType.USER_ADDED, clientName, SERVER_USER);
                    sendBroadcastMessage(messageAdd);
                    printMessage.writeMessage(String.format("%s присоединился к серверу", clientName));
                    notifyAddUser(connection, clientName);
                    sendRecentMessages(connection);
                    if (botClient.isRun()) botClient.clientsMessage(clientName);
                    serverMessageLoop(connection);
                } catch (IOException e) {
                    printMessage.writeMessage("Ошибка работы с клиентом " + e.getMessage());
                    logger.error("Ошибка работы с клиентом " + e.getMessage());
                }
                if (clientName != null) {
                    connectionsMap.remove(clientName);
                    sendBroadcastMessage(new MessageImpl(MessageType.USER_REMOVED, clientName, SERVER_USER));
                }
                printMessage.writeMessage(String.format("Соединение с удаленным адресом (%s) закрыто.", socket.getRemoteSocketAddress()));
                logger.info("Соединение с удаленным адресом {} закрыто", socket.getRemoteSocketAddress());
            }
        }
    }
