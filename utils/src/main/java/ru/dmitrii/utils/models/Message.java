package ru.dmitrii.utils.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public interface Message extends Serializable {
    int getId();

    void setId(int id);

    MessageType getType();

    void setType(MessageType type);

    String getData();

    void setData(String data);

    LocalDateTime getDateTime();

    void setDateTime(LocalDateTime dateTime);

    User getAuthor();

    void setAuthor(User author);
}
