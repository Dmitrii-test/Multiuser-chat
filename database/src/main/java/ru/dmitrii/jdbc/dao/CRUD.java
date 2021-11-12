package ru.dmitrii.jdbc.dao;


/**
 *  Интерфейс CRUD для DAO
 */
public interface CRUD<T> {

    int save(T t);

    T show(int id);

    void update(int id, T t);

    void delete(int id);

}
