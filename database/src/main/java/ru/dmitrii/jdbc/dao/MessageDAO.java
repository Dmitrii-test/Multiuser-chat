package ru.dmitrii.jdbc.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import utils.models.Message;

@Component
public class MessageDAO implements CRUD<Message>{
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MessageDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int save(Message message) {
        jdbcTemplate.update("INSERT INTO messages (type, data, datetime, user_id) VALUES(CAST(? AS ms_type), ?, ?, ?)",
                message.getType().toString(), message.getData(), message.getDateTime(), message.getAuthor().getId());
        return 0;
    }

    @Override
    public Message show(int Id) {
        return null;
    }

    @Override
    public void update(int id, Message message) {

    }

    @Override
    public void delete(int id) {

    }
}
