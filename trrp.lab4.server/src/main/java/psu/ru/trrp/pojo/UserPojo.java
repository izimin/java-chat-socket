package psu.ru.trrp.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/*
 * Класс пользователя чата
 * */

@Getter
@AllArgsConstructor
public class UserPojo implements Serializable {
    private UUID id;
    private String userName;
    private String hash;
    private String salt;
}