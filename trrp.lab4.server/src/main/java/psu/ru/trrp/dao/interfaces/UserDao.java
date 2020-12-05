package psu.ru.trrp.dao.interfaces;

import psu.ru.trrp.pojo.UserPojo;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface UserDao {
    void addUser(UserPojo userPojo);

    UserPojo getUser(String userName, String password) throws NoSuchAlgorithmException, InvalidKeySpecException;
}
