package ru.dmitrii.jdbc.dao;

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.RowMapper;
import ru.dmitrii.utils.models.User;
import ru.dmitrii.utils.models.UserImpl;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements RowMapper<User> {
    @Override
    public User mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
        User user = new UserImpl();
        user.setId(rs.getInt("id_user"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));
        return user;
    }
}