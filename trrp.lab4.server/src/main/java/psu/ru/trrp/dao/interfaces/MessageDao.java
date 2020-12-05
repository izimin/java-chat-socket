package psu.ru.trrp.dao.interfaces;

import psu.ru.trrp.pojo.MessagePojo;

import java.util.List;

public interface MessageDao {
    List<MessagePojo> getListMessages();

    void addMessage(MessagePojo message);
}