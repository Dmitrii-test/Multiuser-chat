package ru.dmitrii.jdbc.dao;

import org.springframework.transaction.annotation.Transactional;
import ru.dmitrii.utils.models.User;

public interface UserDao extends CRUD<User> {

    /**
     * Возвращает индекс пользователя, если не найден 0
     * @param name String
     * @return int
     */
    int indexUser(String name);

    /**
     * Вернуть User по его id
     * @param id int
     * @return User
     */
    User show(int id);

    /**
     * Если пользователя нет возвращает true
     * @param name String
     * @return boolean
     */
    boolean checkNoUser(String name);

    /**
     * Если пользователя admin true
     * @param name String
     * @return boolean
     */
    boolean checkAdmin(String name);

    /**
     * Если пользователь и пароль правильные возвращает true
     * @param name String
     * @param password String
     * @return boolean
     */
    boolean checkUser(String name, String password);

    /**
     * Сохранить User
     * @param user User
     * @return int id пользователя
     */
    int save(User user);

    /**
     * Обновить данные пользователя
     * @param id int
     * @param updatedUser User
     */
    void update(int id, User updatedUser);

    @Transactional
    void updateAdmin(int id);

    /**
     * Удалить пользователя
     * @param id int
     */
    void delete(int id);
}
