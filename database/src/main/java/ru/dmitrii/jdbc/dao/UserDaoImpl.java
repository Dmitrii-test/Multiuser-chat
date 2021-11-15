package ru.dmitrii.jdbc.dao;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.dmitrii.utils.models.User;

@Repository
public class UserDaoImpl implements UserDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsertUsers;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    @Autowired
    public UserDaoImpl(JdbcTemplate jdbcTemplate, SimpleJdbcInsert simpleJdbcInsertUsers,
                       PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsertUsers = simpleJdbcInsertUsers;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Возвращает индекс пользователя, если не найден 0
     *
     * @param name String
     * @return int
     */
    @Override
    @Transactional(readOnly = true)
    public int indexUser(String name) {
        Integer integer = jdbcTemplate.queryForObject("SELECT id_user FROM users" +
                " where name=?", Integer.class, name);
        if (integer == null) return 0;
        return integer;
    }

    /**
     * Вернуть User по его id
     *
     * @param id int
     * @return User
     */
    @Override
    @Transactional(readOnly = true)
    public User show(int id) throws NullPointerException{
        return jdbcTemplate.queryForObject("SELECT * FROM users WHERE id_user=?", new UserMapper(), id);
    }

    /**
     * Если пользователя нет возвращает true
     *
     * @param name String
     * @return boolean
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkNoUser(String name) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users WHERE name=?", Integer.class, name);
        return count == null || count == 0;
    }

    /**
     * Если пользователя admin true
     *
     * @param name String
     * @return boolean
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkAdmin(String name) {
        String role = jdbcTemplate.queryForObject("select role from users WHERE name=?", String.class, name);
        if (role != null) return role.equals("admin");
        else return false;
    }

    /**
     * Если пользователь и пароль правильные возвращает true
     *
     * @param name     String
     * @param password String
     * @return boolean
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkUser(String name, String password) {
        String pass = jdbcTemplate.queryForObject("select password from users WHERE name=?",
                String.class, name);
        return passwordEncoder.matches(password, pass);
    }

    /**
     * Сохранить User
     *
     * @param user User
     * @return int id пользователя
     */
    @Override
    @Transactional
    public int save(@NotNull User user) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", user.getName())
                .addValue("password", passwordEncoder.encode(user.getPassword()));
        Number id = null;
        try {
            id = simpleJdbcInsertUsers.executeAndReturnKey(params);
        } catch (Exception e) {
            logger.error("Ошибка сохранения {} " + e.getMessage(), user);
        }
        if (id != null) {
            return id.intValue();
        }
        else return 0;
    }

    /**
     * Обновить данные пользователя
     *
     * @param id          int
     * @param updatedUser User
     */
    @Override
    @Transactional
    public void update(int id, @NotNull User updatedUser) {
        try {
            jdbcTemplate.update("UPDATE users SET name=?, password=? WHERE id_user=?", updatedUser.getName(),
                    passwordEncoder.encode(updatedUser.getPassword()), id);
        } catch (DataAccessException e) {
            logger.error("Ошибка обновления id {} " + e.getMessage(), id);
        }
    }

    /**
     * Обновить данные пользователя
     *
     * @param id int
     */
    @Override
    @Transactional
    public void updateAdmin(int id) {
        try {
            jdbcTemplate.update("UPDATE users SET role=CAST(? AS my_state) WHERE id_user=?", "admin", id);
        } catch (DataAccessException e) {
            logger.error("Ошибка присвоения admin id {} " + e.getMessage(), id);
        }
    }

    /**
     * Удалить пользователя
     *
     * @param id int
     */
    @Override
    @Transactional
    public void delete(int id) {
        try {
            jdbcTemplate.update("DELETE FROM users WHERE id_user=?", id);
        } catch (DataAccessException e) {
            logger.error("Ошибка удаления id {} "+ e.getMessage(),id);
        }
    }
}
