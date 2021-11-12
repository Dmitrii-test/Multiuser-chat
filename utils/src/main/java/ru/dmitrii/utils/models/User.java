package ru.dmitrii.utils.models;

import java.io.Serializable;
import java.util.List;

public interface User extends Serializable {
    int getId();

    void setId(int id);

    String getName();

    void setName(String name);

    String getPassword();

    void setPassword(String password);

    List<Message> getMessages();

    void setMessages(List<Message> messages);
}
