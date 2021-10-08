package ru.dmitrii.jdbc.dao;

import org.springframework.jdbc.core.RowMapper;
import utils.models.User;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("first_name"));
        user.setPassword(rs.getString("adress"));
        return user;

    }
}