package ru.dmitrii.jdbc.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import utils.models.Message;
import utils.models.MessageType;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MessageMapper implements RowMapper<Message> {

    private final UserDAO userDAO;

    public MessageMapper(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setType(MessageType.valueOf(rs.getString("type")));
        message.setDateTime(rs.getTimestamp("datetime").toLocalDateTime());
        message.setAuthor(userDAO.show(rs.getInt("user_id")));
        return message;
    }
}
