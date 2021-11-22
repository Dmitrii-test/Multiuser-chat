package ru.dmitrii.client.gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientGuiModel {
    private final Set<String> allUserNames = new HashSet<>();
    private String newMessage;

    public Set<String> getAllUserNames() {
        return Collections.unmodifiableSet(allUserNames);
    }

    public String getNewMessage() {
        return newMessage;
    }

    /**
     * Установить новое сообщение
     * @param newMessage String
     */
    public void setNewMessage(String newMessage) {
        this.newMessage = newMessage;
    }

    /**
     * Добавить имя пользователя в Set
     * @param newUserName String
     */
    public void addUser(String newUserName) {
        allUserNames.add(newUserName);
    }

    /**
     * Удалить пользователя из Set
     * @param userName String
     */
    public void deleteUser(String userName) {
        allUserNames.remove(userName);
    }

}
