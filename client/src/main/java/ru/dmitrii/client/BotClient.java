package ru.dmitrii.client;


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
        botClient.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + ((int) (Math.random() * 100));
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMessageLoop() throws IOException{
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
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
                        dateformat = "d.MM.YYYY";
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
                    case "минуты":
                        dateformat = "m";
                        break;
                    case "секунды":
                        dateformat = "s";
                        break;
                }
                if (dateformat != null) {
                    String reply = String.format("Информация для %s: %s",
                            strings[0],
                            new SimpleDateFormat(dateformat).format(Calendar.getInstance().getTime())
                    );
                    sendTextMessage(reply);
                }
            }
        }
    }
}