package ru.dmitrii.jdbc.dao;

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.dmitrii.utils.models.Message;
import ru.dmitrii.utils.models.MessageImpl;
import ru.dmitrii.utils.models.MessageType;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MessageMapper implements RowMapper<Message> {

    private final UserDao userDAO;

    public MessageMapper(UserDao userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public Message mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException, NullPointerException {
        Message message = new MessageImpl();
        message.setId(rs.getInt("id_message"));
        message.setType(MessageType.valueOf(rs.getString("type")));
        message.setData(rs.getString("data"));
        message.setDateTime(rs.getTimestamp("datetime").toLocalDateTime());
        message.setAuthor(userDAO.show(rs.getInt("user_id")));
        return message;
    }
}
