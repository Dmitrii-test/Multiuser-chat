package ru.dmitrii.client;


import ru.dmitrii.utils.models.User;
import ru.dmitrii.utils.printers.ConsolePrinter;
import ru.dmitrii.utils.printers.PrintMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BotClient extends Client {
    private static final PrintMessage handler = new ConsolePrinter();

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.currentUser = new User(3,"bot", "bot_user");
        botClient.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected String getUserName() {
        return currentUser.getName();
    }

    @Override
    protected String getPassword() {
        return currentUser.getPassword();
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMessageLoop() throws IOException{
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час.");
            super.clientMessageLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            handler.writeMessage(message);
            String[] strings = message.split(": ");
            if (strings.length == 2) {
                String dateformat = null;
                switch (strings[1].toLowerCase(Locale.ROOT)) {
                    case "дата":
                        dateformat = "dd.MM.YYYY";
                        break;
                    case "день":
                        dateformat = "d";
                        break;
                    case "месяц":
                        dateformat = "MMMM";
                        break;
                    case "год":
                        dateformat = "YYYY";
                        break;
                    case "время":
                        dateformat = "H:mm:ss";
                        break;
                    case "час":
                        dateformat = "H";
                        break;
                }
                if (dateformat != null) {
                    String reply = String.format("Информация для %s: %s",
                            strings[0].substring(0, strings[0].indexOf(" ")),
                            new SimpleDateFormat(dateformat).format(Calendar.getInstance().getTime())
                    );
                    sendTextMessage(reply);
                }
            }
        }
    }
}