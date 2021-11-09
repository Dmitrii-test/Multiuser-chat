package ru.dmitrii.server.bot;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.dmitrii.server.Server;
import ru.dmitrii.utils.models.Message;
import ru.dmitrii.utils.models.MessageImpl;
import ru.dmitrii.utils.models.MessageType;
import ru.dmitrii.utils.models.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.SynchronousQueue;

@Service
@Lazy
public class BotClient extends Thread {

    private volatile boolean run = false;
    private final User currentUser;
    private final Server server;
    private final SynchronousQueue<Message> queue = new SynchronousQueue<>();


    public BotClient(Server server) {
        this.server = server;
        currentUser = new User(3, "bot", "bot_user");
    }


    protected void sendTextMessage(String text, String name) {
        server.sendUserMessage(new MessageImpl(MessageType.TEXT, text, currentUser), name);
    }

    public void handleMessage(Message message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isRun() {
        return run;
    }

    @Override
    public void run() {
        run = true;
        try {
            clientMessageLoop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляем сообщения для подключившихся
     * @param name String to
     */
    public void clientsMessage(String name) {
        sendTextMessage("Привет чатику. Я бот. Понимаю команды: bot-дата, bot-время, bot-online", name);
    }

    public void stopWork() {
        this.run = false;
    }

    private void processIncomingMessage(Message message) {
        String name = message.getAuthor().getName();
        String data = message.getData();
        String[] strings = data.split("[-_]");
        String response = "не верный запрос";
        if (strings.length == 2) {
            String dateformat = null;
            String s = strings[1].toLowerCase(Locale.ROOT).trim();
            switch (s) {
                case "дата":
                    dateformat = "dd.MM.YYYY";
                    break;
                case "время":
                    dateformat = "H:mm:ss";
                    break;
                case "online":
                    response = String.valueOf(server.getOnline());
                    break;
            }
            if (dateformat != null) {
                response = new SimpleDateFormat(dateformat).format(Calendar.getInstance().getTime());
            }
            sendTextMessage(String.format("Информация для %s: %s  %s", name, s, response), name);
        }
    }

    private void clientMessageLoop() throws InterruptedException {
        while (run) {
            Message message = queue.take();
            if (message.getType() == MessageType.TEXT) {
                processIncomingMessage(message);
            }
        }
    }
}
