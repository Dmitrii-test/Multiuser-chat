package ru.dmitrii.jdbc.dao;

import org.springframework.transaction.annotation.Transactional;
import ru.dmitrii.utils.models.Message;

import java.util.List;

public interface MessageDao extends CRUD<Message> {

    /**
     * Возвращает индекс сообщения, если не найден 0
     *
     * @param message Message
     * @return int
     */
    @Transactional(readOnly = true)
    int indexMessage(Message message);

    /**
     * Сохранить сообщение в DB
     *
     * @param message Message
     * @return int
     */
    @Override
    int save(Message message);

    /**
     * Получить сообщение по id
     *
     * @param id int
     * @return Message
     */
    @Override
    Message show(int id);

    /**
     * Получить сортированный список сообщений за i часов
     *
     * @param i int
     * @return List<Message>
     */
    List<Message> showMessageTime(int i);

    /**
     * Получить отсортированный список сообщений пользователя
     *
     * @param id int
     * @return List<Message>
     */
    List<Message> showAll(int id);

    /**
     * Обновить сообщение
     *
     * @param id      int
     * @param message Message
     */
    @Override
    void update(int id, Message message);

    /**
     * Удалить сообщение
     *
     * @param id int
     */
    @Override
    void delete(int id);
}
