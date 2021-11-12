package ru.dmitrii.utils.printers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Класс для работы IO
 */
@Service
public class ConsolePrinter implements PrintMessage {
    private final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    private static final Logger logger = LoggerFactory.getLogger(ConsolePrinter.class);

    @Override
    public void writeMessage(String message) {
        System.out.println(message);
    }

    @Override
    public String readString() {
        String S = "";
        while (S.equals("")) {
            try {
                S = bufferedReader.readLine();
            } catch (IOException e) {
                System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
                logger.error("Произошла ошибка при попытке ввода текста {}", S);
            }
        }
        return S;
    }

    @Override
    public int readInt() {
        int i = 0;
        while (i == 0) {
            try {
                i = Integer.parseInt(readString());
            } catch (NumberFormatException e) {
                System.out.println("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
                logger.error("Произошла ошибка при попытке ввода числа {}", i);
            }
        }
        return i;
    }
}
