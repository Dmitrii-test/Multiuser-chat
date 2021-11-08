package ru.dmitrii.utils.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private  int id;
    private  MessageType type;
    private  String data;
    private  LocalDateTime dateTime;
    private  User author;

    public Message() {
    }

    public Message(String data, LocalDateTime dateTime, User author) {
        this.data = data;
        this.dateTime = dateTime;
        this.author = author;
    }

    public Message(MessageType type, String data, User author) {
        this.type = type;
        this.data = data;
        dateTime = LocalDateTime.now();
        this.author = author;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", type=" + type +
                ", data='" + data + '\'' +
                ", dateTime=" + dateTime +
                ", author=" + author +
                '}';
    }
}
