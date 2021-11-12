package ru.dmitrii.utils.models;

import java.util.List;

public class UserImpl implements User {
    private int id;
    private String name;
    private String password;
    private List<Message> messages;

    public UserImpl() {
    }

    public UserImpl(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public UserImpl(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name;
    }
}
