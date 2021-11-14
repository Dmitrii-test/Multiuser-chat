package ru.dmitrii.server.bot;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.dmitrii.jdbc.dao.MessageDao;
import ru.dmitrii.jdbc.dao.UserDao;
import ru.dmitrii.server.Server;
import ru.dmitrii.utils.models.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.SynchronousQueue;

@Service
@Lazy
public class ChatBot extends Thread {

    private volatile boolean run = false;
    private final User currentUser;
    private final Server server;
    private final SynchronousQueue<Message> queue = new SynchronousQueue<>();
    private final UserDao userDAO;
    private final MessageDao messageDao;
    private final Logger logger = LoggerFactory.getLogger(ChatBot.class);


    public ChatBot(Server server, UserDao userDAO, MessageDao messageDAO) {
        this.server = server;
        this.userDAO = userDAO;
        this.messageDao = messageDAO;
        currentUser = new UserImpl(3, "bot", "bot_user");
    }

    @Override
    public void run() {
        run = true;
        logger.info("Запущен бот");
        try {
            clientMessageLoop();
        } catch (InterruptedException e) {
            logger.warn("Ошибка работы чат-бота" + e.getMessage());
        }
    }


    /**
     * Отправить сообщение пользователю
     *
     * @param text String
     * @param name String
     */
    protected void sendTextMessage(String text, String name) {
        server.sendUserMessage(new MessageImpl(MessageType.TEXT, text, currentUser), name);
    }

    /**
     * Вставить message в очередь
     *
     * @param message Message
     */
    public void handleMessage(Message message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            logger.warn("Ошибка работы с очередью " + e.getMessage());
        }
    }

    /**
     * Получаем Message из очереди
     *
     * @throws InterruptedException InterruptedException
     */
    private void clientMessageLoop() throws InterruptedException {
        while (run) {
            Message message = queue.take();
            if (message.getType() == MessageType.TEXT) {
                processIncomingMessage(message);
            }
        }
    }

    /**
     * Получить состояние службы
     *
     * @return boolean
     */
    public boolean isRun() {
        return run;
    }

    /**
     * Остановить службу
     */
    public void stopWork() {
        this.run = false;
    }

    /**
     * Отправляем сообщения для подключившихся
     *
     * @param name String to
     */
    public void clientsMessage(String name) {
        sendTextMessage("Привет чату. Я бот. Понимаю команды: bot-дата, bot-время, bot-online, bot-admin", name);
    }


    /**
     * Обработать запрос и вернуть ответ
     *
     * @param message Message
     */
    private void processIncomingMessage(@NotNull Message message) {
        int id = message.getAuthor().getId();
        String name = message.getAuthor().getName();
        String data = message.getData();
        String[] strings = data.split("[-_]");
        String response = "не верный запрос";
        if (strings.length == 2) {
            String dateformat = null;
            String s = strings[1].toLowerCase(Locale.ROOT).trim();
            switch (s) {
                // общие команды бота
                case "дата":
                    dateformat = "dd.MM.YYYY";
                    break;
                case "время":
                    dateformat = "H:mm:ss";
                    break;
                case "online":
                    response = String.valueOf(server.getOnline());
                    break;
                case "admin":
                    response = checkAdmin(name);
            }
            if (dateformat != null) {
                response = new SimpleDateFormat(dateformat).format(Calendar.getInstance().getTime());
            }
            sendTextMessage(String.format("Информация для %s: команда '%s' -  %s", name, s, response), name);
            logger.info("Информация для {}: команда '{}' -  {}", name, s, response);
        }
        // команды админа
        if (strings.length == 3) {
            String res = strings[1].toLowerCase(Locale.ROOT).trim();
            String val = strings[2].trim().toLowerCase();
            switch (res) {
                case "changepassword":
                    userDAO.update(id, new UserImpl(name, val));
                    sendTextMessage(String.format("Пароль сменён у %s", name), name);
                    break;
                case "showall":
                    if (checkUser(name, val)) break;
                    List<Message> messageList = messageDao.showAll(userDAO.indexUser(val));
                    if (messageList != null) messageList.forEach(m -> sendTextMessage(m.getData(), name));
                    else sendTextMessage(String.format("Не найдены сообщения %s", val), name);
                    break;
                case "deleteuser":
                    if (checkUser(name, val)) break;
                    userDAO.delete(userDAO.indexUser(val));
                    sendTextMessage(String.format("Пользователь %s удалён", val), name);
                    break;
                case "setadmin":
                    if (checkUser(name, val)) break;
                    userDAO.updateAdmin(userDAO.indexUser(val));
                    sendTextMessage(String.format("Пользователь %s стал админом", val), name);
                    break;
                default:
                    sendTextMessage(String.format("Информация для %s: команда '%s' -  %s", name, res, response), name);
                    logger.info("Информация для {}: команда '{}' -  {}", name, res, response);
            }
        }

    }

    /**
     * Проверка есть ли пользователь
     *
     * @param name String
     * @param val  String
     * @return boolean
     */
    private boolean checkUser(String name, String val) {
        if (userDAO.checkNoUser(name)) {
            sendTextMessage(String.format("Пользователь %s не найден", val), name);
            return true;
        }
        return false;
    }

    /**
     * Команды админа
     *
     * @param name String
     * @return String
     */
    @NotNull
    private String checkAdmin(String name) {
        boolean admin = userDAO.checkAdmin(name);
        if (admin) return "Вам доступны команды bot-changePassword - новый пароль, bot-showAll- имя пользователя, \n" +
                " bot-deleteUser - имя пользователя, bot-setAdmin - имя пользователя";
        else return "Вам доступна команда bot-changePassword-новый пароль";
    }
}
