package ru.dmitrii.jdbc.dao;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.dmitrii.utils.models.Message;

import java.util.Comparator;
import java.util.List;

@Repository
public class MessageDaoImpl implements MessageDao {
    private final JdbcTemplate jdbcTemplate;
    private final MessageMapper messageMapper;
    private static final Logger logger = LoggerFactory.getLogger(MessageDaoImpl.class);


    @Autowired
    public MessageDaoImpl(JdbcTemplate jdbcTemplate, MessageMapper messageMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.messageMapper = messageMapper;
    }

    /**
     * Возвращает индекс сообщения, если не найден 0
     *
     * @param message Message
     * @return int
     */
    @Override
    @Transactional(readOnly = true)
    public int indexMessage(@NotNull Message message) {
        Integer integer = jdbcTemplate.queryForObject("SELECT id_message FROM messages" +
                " where data = ? AND user_id = ? ", Integer.class, message.getData(), message.getAuthor());
        if (integer == null) return 0;
        return integer;
    }

    /**
     * Сохранить сообщение в DB
     *
     * @param message Message
     * @return int
     */
    @Override
    @Transactional
    public int save(@NotNull Message message) {
        return jdbcTemplate.update("INSERT INTO messages (type, data, datetime, user_id) " +
                        "VALUES(CAST(? AS ms_type), ?, ?, ?)",
                message.getType().toString(), message.getData(), message.getDateTime(), message.getAuthor().getId());
    }

    /**
     * Получить сообщение по id
     *
     * @param id int
     * @return Message
     */
    @Override
    @Transactional(readOnly = true)
    public Message show(int id) {
        return jdbcTemplate.queryForObject("SELECT * FROM messages WHERE id_message=?", messageMapper, id);
    }

    /**
     * Получить сортированный список сообщений за i часов
     *
     * @param i int
     * @return List<Message>
     */
    @Override
    @Transactional(readOnly = true)
    public List<Message> showMessageTime(int i) {
        String sql =
                String.format("SELECT * FROM messages WHERE datetime >= NOW() - interval '%d hour' AND type='TEXT'", i);
        List<Message> messageList = jdbcTemplate.query(sql, messageMapper);
        messageList.sort(getMessageComparator());
        return messageList;
    }

    /**
     * Получить отсортированный список сообщений пользователя
     *
     * @param id int
     * @return List<Message>
     */
    @Override
    @Transactional(readOnly = true)
    public List<Message> showAll(int id) {
        List<Message> messageList = jdbcTemplate.query("SELECT * FROM messages WHERE user_id = ? AND type='TEXT'",
                    messageMapper, id);
            messageList.sort(getMessageComparator());
            return messageList;
    }

    /**
     * Компоратор сортировки сообщений по дате
     *
     * @return Comparator<Message>
     */
    @NotNull
    private Comparator<Message> getMessageComparator() {
        return (o1, o2) -> {
            if (o1.getDateTime() == null || o2.getDateTime() == null)
                return 0;
            return o1.getDateTime().compareTo(o2.getDateTime());
        };
    }

    /**
     * Обновить сообщение
     *
     * @param id      int
     * @param message Message
     */
    @Override
    @Transactional
    public void update(int id, @NotNull Message message) {
        try {
            jdbcTemplate.update("UPDATE messages SET type=?, data=?, datetime =?, user_id =? WHERE id_message=?",
                    message.getType(), message.getData(), message.getDateTime(), message.getAuthor().getId(), id);
        } catch (DataAccessException e) {
            logger.error("Ошибка обновления id {} " + e.getMessage(), id);
        }
    }

    /**
     * Удалить сообщение
     *
     * @param id int
     */
    @Override
    @Transactional
    public void delete(int id) {
        try {
            jdbcTemplate.update("DELETE FROM messages WHERE id_message=?", id);
        } catch (DataAccessException e) {
            logger.error("Ошибка удаления id {} " + e.getMessage(), id);
        }
    }
}
