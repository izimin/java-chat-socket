package psu.ru.trrp.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/*
 * Класс сообщения
 * */

@Getter
@AllArgsConstructor
public class MessagePojo implements Serializable {
    private UUID id;
    private String text;
    private LocalDateTime sentDate;
    private UUID userId;
    private String userName;
}
