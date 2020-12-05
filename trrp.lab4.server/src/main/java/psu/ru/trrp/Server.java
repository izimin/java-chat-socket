package psu.ru.trrp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import psu.ru.trrp.dao.MessageDaoImpl;
import psu.ru.trrp.dao.UserDaoImpl;
import psu.ru.trrp.dao.interfaces.MessageDao;
import psu.ru.trrp.dao.interfaces.UserDao;
import psu.ru.trrp.pojo.MessagePojo;
import psu.ru.trrp.pojo.UserPojo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Server {
    private static Map<String, Channel> queues = new HashMap<>();

    public static void main(String[] args) {
        // Порты, на которых может быть открыт сервер
        final int START_PORT = 33333;
        final int FINISH_PORT = 33340;

        ServerSocket serverSocket = null;
        // Пробуем запустить на разынх портах (для запуска нескольких серверов одновременно)
        for (int port = START_PORT; port < FINISH_PORT; port++) {
            try {
                serverSocket = new ServerSocket(port);
                break;
            } catch (IOException e) {
                System.out.println(String.format("Порт %d занят!", port));
            }
        }

        if (serverSocket == null) {
            System.out.println("Не удалось запустить сервер :(");
            return;
        } else {
            System.out.println(String.format("Сервер запущени нв порту %d", serverSocket.getLocalPort()));
        }

        // Поток работы с клиентами
        ServerSocket finalServerSocket = serverSocket;
        new Thread(() -> {
            while (true) {
                Socket socket;
                try {
                    socket = finalServerSocket.accept();
                    // Потор работы клиента
                    new Thread(new HandleAClient(socket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("Чтобы остановить сервер нажмите Enter");
        new Scanner(System.in).next();
    }

    static class HandleAClient implements Runnable {
        private Socket socket;
        private UserDao userDao;
        private MessageDao messageDao;

        HandleAClient(Socket socket) {
            this.socket = socket;
            this.userDao = new UserDaoImpl();
            this.messageDao = new MessageDaoImpl();
        }

        public void run() {
            System.out.println("Клиент подключился");
            try {
                // Инициализируем стримы для общения с клиентами по сокетам
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject("Hello");

                UserPojo curUser = null;

                // Ждем пока пользователь введет верный логин и пароль
                while (curUser == null) {
                    UserPojo user = (UserPojo) ois.readObject();
                    if (user.getSalt().isEmpty()) {
                        curUser = userDao.getUser(user.getUserName(), user.getHash());
                        oos.writeObject(curUser);
                    } else {
                        curUser = user;
                    }
                }

                // Отправляем список сообщений клиенту
                oos.writeObject(messageDao.getListMessages());

                // Слхжаем канал очереди сообщений для отправки сообщений клиенту
                Channel channelSend = null;
                ConnectionFactory factory = new ConnectionFactory();

                factory.setHost("localhost");
                factory.setUsername("Ilya");
                factory.setPassword("Ilya");

                String queueName = curUser.getUserName() + "Chat";

                try {
                    channelSend = factory.newConnection().createChannel();
                    channelSend.queueDeclare(queueName, false, false, false, null);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }


                // Если клиент подключается впервые, добавляем имя очереди и канал в коллекцию
                queues.putIfAbsent(queueName, channelSend);

                while (true) {
                    // Ждем пока пользователь отправит сообщение
                    MessagePojo msg = (MessagePojo) ois.readObject();
                    if (msg == null) {
                        continue;
                    }

                    // Добавляем сообщение в БД
                    messageDao.addMessage(msg);

                    // Кладем сообщение в очередь каждому из клиентов
                    for (Map.Entry<String, Channel> entry : queues.entrySet()) {
                        if (entry.getKey().equals(queueName)) {
                            continue;
                        }
                        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
                            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                                o.writeObject(msg);
                            }
                            entry.getValue().basicPublish("", entry.getKey(), null, b.toByteArray());
                        }
                    }
                }

            } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}