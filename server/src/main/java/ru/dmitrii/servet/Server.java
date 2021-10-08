package ru.dmitrii.servet;

import utils.Connection;
import utils.models.Message;
import utils.models.MessageType;
import utils.printers.ConsolePrinter;
import utils.printers.PrintMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static private final Map<String, Connection> CONNECTION_MAP = new ConcurrentHashMap<>();
    static private final PrintMessage PRINT_MESSAGE = new ConsolePrinter();
    static private final List<Handler> handlerList = new ArrayList<>();

    public static void main(String[] args) {
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
     * @param serverSocket ServerSocket
     * @return Thread
     */
    private static Thread getThreadSockets(ServerSocket serverSocket) {
        return new Thread(() -> {
                    // accept ждёт пока кто-либо не захочет подсоединится к нему. возврашает Socet
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Handler handlerSocket = new Handler(serverSocket.accept());
                            handlerList.add(handlerSocket);
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
        handlerList.forEach(Thread::interrupt);
        CONNECTION_MAP.forEach((k, v) -> {
            try {
                v.send(new Message(MessageType.SERVER_DISCONNECT, "Выключение сервера"));
                v.close();
            } catch (IOException e) {
                PRINT_MESSAGE.writeMessage("Ошибка закрытия соединения " + e.getMessage());
            }
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
            String name = null;
            while (!accepted) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if (message.getType() == MessageType.USER_NAME) {
                    name = message.getData();
                    if (!name.isEmpty() && CONNECTION_MAP.get(name) == null) {
                        CONNECTION_MAP.putIfAbsent(name, connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED));
                        accepted = true;
                    }
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
                    connection.send(new Message(MessageType.USER_ADDED, clientName));
                }
            }
        }

        /**
         * Цикл приема и обработки сообщений Text
         * @param connection Connection
         * @param userName String
         * @throws IOException IOException
         */
        private void serverMessageLoop(Connection connection, String userName) throws IOException {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message message = connection.receive();
                    if (message.getType() == MessageType.TEXT) {
                        String messageText = userName + ": " + message.getData();
                        PRINT_MESSAGE.writeMessage(messageText);
                        sendBroadcastMessage(new Message(MessageType.TEXT, messageText));
                    } else PRINT_MESSAGE.writeMessage(
                            String.format("Ошибка! Недопустимый тип сообщения (MessageType.%s) от клиента: %s",
                                    message.getType().toString(), userName));
                }catch (SocketException ignored) {
                }
            }

        }

        @Override
        public void run() {
            PRINT_MESSAGE.writeMessage("Установлено соединение с удаленным клиентом с адресом: " +
                    socket.getRemoteSocketAddress());
            Connection connection;
            String clientName = null;
            try {
                connection = new Connection(socket);
                clientName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, clientName));
                PRINT_MESSAGE.writeMessage(String.format("%s присоединился к серверу", clientName));
                notifyAddUser(connection, clientName);
                serverMessageLoop(connection, clientName);
            } catch (IOException e) {
                PRINT_MESSAGE.writeMessage("Ошибка установки соединения " + e.getMessage());
            }
            if (clientName != null) {
                CONNECTION_MAP.remove(clientName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, clientName));
            }
            PRINT_MESSAGE.writeMessage(String.format("Соединение с удаленным адресом (%s) закрыто.", socket.getRemoteSocketAddress()));
        }

    }
}
