package utils.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private final MessageType type;
    private final String data;
    private final LocalDateTime dateTime;
    private final User author;


    public Message(MessageType type, User author) {
        this.type = type;
        this.data = "";
        this.dateTime = LocalDateTime.now();
        this.author = author;

    }

    public Message(MessageType type, String data, User author) {
        this.type = type;
        this.data = data;
        dateTime = LocalDateTime.now();
        this.author = author;
    }


    public MessageType getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public User getAuthor() {
        return author;
    }
}
