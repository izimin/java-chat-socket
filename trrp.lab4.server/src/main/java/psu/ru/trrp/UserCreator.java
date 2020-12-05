package psu.ru.trrp;

import psu.ru.trrp.dao.UserDaoImpl;
import psu.ru.trrp.dao.interfaces.UserDao;
import psu.ru.trrp.pojo.UserPojo;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;


// Приложение, которое позволяет добавить нового пользователя чата
public class UserCreator {
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Username: ");
        String userName = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        // Salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        // hash
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = factory.generateSecret(spec).getEncoded();

        UserDao userDao = new UserDaoImpl();
        userDao.addUser(new UserPojo(UUID.randomUUID(), userName,
                Base64.getEncoder().encodeToString(hash),
                Base64.getEncoder().encodeToString(salt)));
    }
}