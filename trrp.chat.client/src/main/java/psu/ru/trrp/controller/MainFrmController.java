package psu.ru.trrp.controller;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.sun.org.apache.bcel.internal.generic.FADD;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import psu.ru.trrp.pojo.ChatItem;
import psu.ru.trrp.pojo.MessagePojo;
import psu.ru.trrp.pojo.UserPojo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainFrmController {

    // Компоненты формы

    @FXML
    private AnchorPane sendAnch;

    @FXML
    private Label lblReconnecting;

    @FXML
    private VBox vChat;

    @FXML
    private ScrollPane scrollChat;

    @FXML
    private Button btSendMsg;

    @FXML
    private Button btSettings;

    @FXML
    private Button btScrollDown;

    @FXML
    private TextArea taMsg;

    private UserPojo userPojo;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    @FXML
    void initialize(Socket socket, UserPojo userPojo, ObjectInputStream ois, ObjectOutputStream oos) {
        this.userPojo = userPojo;
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;

        start(userPojo, this.ois);

        // Подписка на событие клика по кнопке "Отправить сообщение"
        btSendMsg.setOnAction(event -> {
            sendMessage(userPojo, this.oos);
        });

        // Подписка на событие изменения высоты контейнера диалога
        vChat.heightProperty().addListener(observable -> scrollChat.setVvalue(1D));

        // Подписка на событие изменение положения скролла
        scrollChat.vvalueProperty().addListener(observable -> {
            if (scrollChat.getVvalue() != 1D) {
                btScrollDown.setVisible(true);
            } else {
                btScrollDown.setVisible(false);
            }
        });

        // Подписка на прокрутку
        btScrollDown.setOnAction(event -> {
            scrollChat.setVvalue(1D);
        });

        // Отправка сообщения по ctrl + enter
        taMsg.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    sendMessage(userPojo, this.oos);
                }
            }
        });
    }

    private void start(UserPojo userPojo, ObjectInputStream ois) {
        try {
            // Получаем список сообщений из БД
            List<MessagePojo> messageList = (List<MessagePojo>) ois.readObject();
            vChat.getChildren().clear();
            for (MessagePojo messagePojo : messageList) {
                putMsg(messagePojo);
            }

            // Поток, который будет проверять, доступен ли сервер (раз в 3 сек.)
            new Thread(() -> {
                while (true) {
                    try {
                        this.oos.writeObject(null);
                        try {
                            TimeUnit.MILLISECONDS.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        reconnect();
                        return;
                    }
                }
            }).start();

            // Поток очереди сообщений
            new Thread(() -> {
                String queueName = userPojo.getUserName() + "Chat";
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(socket.getInetAddress().getHostAddress());
                factory.setUsername("Ilya");
                factory.setPassword("Ilya");
                Channel channelRecv = null;
                try {
                    channelRecv = factory.newConnection().createChannel();
                    channelRecv.queueDeclare(queueName, false, false, false, null);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    try (ByteArrayInputStream b = new ByteArrayInputStream(delivery.getBody())) {
                        try (ObjectInputStream o = new ObjectInputStream(b)) {
                            MessagePojo messagePojo = (MessagePojo) o.readObject();
                            putMsg(messagePojo);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                };

                try {
                    if (channelRecv != null) {
                        channelRecv.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                        });
                    }
                } catch (IOException e) {
                    reconnect();
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException | ClassNotFoundException e) {
            reconnect();
            e.printStackTrace();
        }
    }

    // Метод отправки сообщения
    private void sendMessage(UserPojo userPojo, ObjectOutputStream oos) {
        // Проверка сообщения на пустоту
        if (taMsg.getText().trim().isEmpty()) {
            return;
        }

        // Создаем объект сообщения
        MessagePojo messagePojo = new MessagePojo(UUID.randomUUID(), taMsg.getText(), LocalDateTime.now(), userPojo.getId(), userPojo.getUserName());
        try {
            // Отпраляем сообщение серверу
            oos.writeObject(messagePojo);
        } catch (IOException e) {
            // В случае неудачи пытаемся переподключиться
            reconnect();
        }

        // Кладем сообщение в контейнек только в том случае, если сокет инициализирован
        if (socket != null) {
            putMsg(messagePojo);
            taMsg.setText("");
        }
    }

    // Метод отображения сообщения на экран
    private void putMsg(MessagePojo message) {
        boolean isFromMe = message.getUserId().equals(userPojo.getId());
        ChatItem chatItem = new ChatItem(message, !isFromMe, false);
        Platform.runLater(() -> {
            vChat.getChildren().add(chatItem.getMyMsgBox());
        });
    }

    // Метод попытки переподключения
    private void reconnect() {

        // Порты, на которых может быть развернут сервер
        final int START_PORT = 33333;
        final int FINISH_PORT = 33340;

        socket = null;

        new Thread(() -> {

            List<InetAddress> addresses = new ArrayList<>();
            Platform.runLater(() -> {
                sendAnch.setDisable(true);
            });

            // Ищем все активные IP адреса в сети
            try {
                Enumeration nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = (NetworkInterface) nis.nextElement();
                    Enumeration ias = ni.getInetAddresses();
                    while (ias.hasMoreElements()) {
                        InetAddress ia = (InetAddress) ias.nextElement();
                        addresses.add(ia);
                    }
                }
            } catch (SocketException ignored) {
            }

            // Пробуем подконнектиться
            for (InetAddress ia : addresses) {
                boolean isConn = false;
                for (int port = START_PORT; port < FINISH_PORT; port++) {
                    try {
                        this.socket = new Socket(ia, port);
                        if (this.socket.isConnected()) {
                            oos = new ObjectOutputStream(this.socket.getOutputStream());
                            ois = new ObjectInputStream(this.socket.getInputStream());
                            String str = (String) ois.readObject();
                            if (str.equals("Hello")) {
                                Platform.runLater(() -> {
                                    sendAnch.setDisable(false);
                                });
                            }
                            System.out.println("yes!");
                            oos.writeObject(userPojo);
                            start(userPojo, ois);
                            isConn = true;
                            break;
                        }
                    } catch (IOException e) {
                        System.out.println("error");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                if (isConn) {
                    break;
                }
            }
        }).start();

        // Пока идет попытка реконнектинга, выводим на экран в отдельном потоке сообщени, что идет переподключение
        // При этом не блокируется ничего, кроме окна ввода сообщения. Пользователь может прокручивать контейней сообщений
        new Thread(() -> {
            Platform.runLater(() -> {
                lblReconnecting.setVisible(true);
            });
            while (this.socket == null) {
                Platform.runLater(() -> {
                    lblReconnecting.setText("Reconnecting .");
                });
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    lblReconnecting.setText("Reconnecting . .");
                });
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    lblReconnecting.setText("Reconnecting . . .");
                });
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Platform.runLater(() -> {
                lblReconnecting.setVisible(false);
            });
        }).start();
    }
}