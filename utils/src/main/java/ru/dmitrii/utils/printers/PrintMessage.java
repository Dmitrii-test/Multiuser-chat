package ru.dmitrii.utils.printers;

public interface PrintMessage {

    /**
     * Метод вывода строки
     * @param message String
     */
    void writeMessage(String message);

    /**
     * Метод ввода строки
     * @return String
     */
    String readString();

    /**
     * Метод ввода цифр
     * @return int
     */
    int readInt();
}
