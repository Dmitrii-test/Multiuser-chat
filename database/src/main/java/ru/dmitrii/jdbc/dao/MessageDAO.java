package ru.dmitrii.jdbc.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.dmitrii.utils.models.Message;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MessageDAO implements CRUD<Message>{
    private final JdbcTemplate jdbcTemplate;
    private final MessageMapper messageMapper;

    @Autowired
    public MessageDAO(JdbcTemplate jdbcTemplate, MessageMapper messageMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.messageMapper = messageMapper;
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


    public List<Message> showMessageTime() {
        return  jdbcTemplate.query("SELECT * FROM messages WHERE datetime >= NOW() - interval '2 hour' AND type='TEXT'",messageMapper);
    }

    @Override
    public void update(int id, Message message) {

    }

    @Override
    public void delete(int id) {

    }
}
