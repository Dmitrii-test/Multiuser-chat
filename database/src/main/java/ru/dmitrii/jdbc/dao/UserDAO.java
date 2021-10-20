package ru.dmitrii.jdbc.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import utils.models.User;

@Component
public class UserDAO implements CRUD<User> {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsertUsers;

    @Autowired
    public UserDAO(JdbcTemplate jdbcTemplate, SimpleJdbcInsert simpleJdbcInsertUsers) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsertUsers = simpleJdbcInsertUsers;
    }

    /**
     * Возвращает индекс пользователя, если не найден 0
     * @param name String
     * @return int
     */
    @Transactional (readOnly = true)
    public int indexUser(String name) {
        Integer integer = jdbcTemplate.queryForObject("SELECT id FROM users where name=?", Integer.class, name);
        if (integer==null) return 0;
        return integer;
    }

    /**
     * Вернуть User по его id
     * @param i int
     * @return User
     */
    @Transactional (readOnly = true)
    public User show(int i) {
        User user = jdbcTemplate.queryForObject("SELECT * FROM users WHERE id=?", new UserMapper(), i);
        if (user == null) throw new NullPointerException();
        return user;
    }

    /**
     * Если пользователя нет возвращает true
     * @param name String
     * @return boolean
     */
    @Transactional (readOnly = true)
    public boolean checkUser(String name) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users WHERE name=?",Integer.class, name);
        return count == null || count == 0;
    }

    /**
     * Если пользователь и пароль правильные возвращает true
     * @param name String
     * @param password String
     * @return boolean
     */
    @Transactional (readOnly = true)
    public boolean checkUser(String name, String password) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users WHERE name=? AND password=?",
                Integer.class, name, password);
        return count != null && count != 0;
    }

    /**
     * Сохранить User
     * @param user User
     * @return int id пользователя
     */
    @Transactional
    public int save(User user) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", user.getName())
                .addValue("password", user.getPassword());
        Number id = simpleJdbcInsertUsers.executeAndReturnKey(params);
        return id.intValue();
    }

    /**
     * Обновить данные пользователя
     * @param id int
     * @param updatedUser User
     */
    @Transactional
    public void update(int id, User updatedUser) {
        jdbcTemplate.update("UPDATE users SET name=?, password=? WHERE id=?", updatedUser.getName(),
                updatedUser.getPassword(), id);
    }

    /**
     * Удалить пользователя
      * @param id int
     */
    @Transactional
    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM users WHERE id=?", id);
    }
}
