package ru.dmitrii.jdbc.dao;


import utils.models.User;

/**
 *  Интерфейс CRUD для DAO
 */
public interface CRUD {

    void save(User user);

    User show(int Id);

    void update(int id, User user);

    void delete(int id);

}
