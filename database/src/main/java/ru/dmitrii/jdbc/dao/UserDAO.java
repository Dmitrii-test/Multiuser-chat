package ru.dmitrii.jdbc.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import utils.models.User;

import java.util.List;

@Component
@ComponentScan
public class UserDAO implements CRUD<User> {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> index() {
        return jdbcTemplate.query("SELECT * FROM users", new BeanPropertyRowMapper<>(User.class));
    }

    public User show(int Id) {
        return (User) jdbcTemplate.queryForObject("SELECT * FROM users WHERE id=?", new UserMapper(), Id);
    }

    public boolean checkUser(String name) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from users WHERE name=?",Integer.class, name);
        return count != null && count != 0;
    }

    public int save(User user) {
        String name = user.getName();
        if (!checkUser(user.getName())) {
            return jdbcTemplate.update("INSERT INTO users (name, password) VALUES(?, ?)", name,
                    user.getPassword());
        }
        else System.out.printf("Пользователь %s уже есть в базe %n", name);
        return 0;
    }

    public void update(int id, User updatedUser) {
        jdbcTemplate.update("UPDATE users SET name=?, password=? WHERE id=?", updatedUser.getName(),
                updatedUser.getPassword(), id);
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM users WHERE id=?", id);
    }
}
