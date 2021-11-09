package ru.dmitrii.utils.models;

import java.time.LocalDateTime;

public class MessageImpl implements Message {
    private  int id;
    private  MessageType type;
    private  String data;
    private  LocalDateTime dateTime;
    private  User author;

    public MessageImpl() {
    }

    public MessageImpl(String data, LocalDateTime dateTime, User author) {
        this.data = data;
        this.dateTime = dateTime;
        this.author = author;
    }

    public MessageImpl(MessageType type, String data, User author) {
        this.type = type;
        this.data = data;
        dateTime = LocalDateTime.now();
        this.author = author;
    }

    @Override
    public int getId() {return id;}

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public MessageType getType() {
        return type;
    }

    @Override
    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(String data) {
        this.data = data;
    }

    @Override
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public User getAuthor() {
        return author;
    }

    @Override
    public void setAuthor(User author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", data='" + data + '\'' +
                ", dateTime=" + dateTime +
                ", author=" + author +
                '}';
    }
}
