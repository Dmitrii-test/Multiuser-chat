package utils.printers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Класс для работы IO
 */
public class ConsolePrinter implements PrintMessage {
    private final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public void writeMessage(String message) {
        System.out.println(message);
    }

    @Override
    public String readString() {
        String S= null;
        while (S==null) {
            try {
                S = bufferedReader.readLine();
            } catch (IOException e) {
                System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
            }
        }
        return S;
    }

    @Override
    public int readInt() {
        Integer i = null;
        while (i==null) {
            try {
                i = Integer.parseInt(readString());
            }
            catch (NumberFormatException e) {
                System.out.println("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
            }
        }
        return i;
    }
}
