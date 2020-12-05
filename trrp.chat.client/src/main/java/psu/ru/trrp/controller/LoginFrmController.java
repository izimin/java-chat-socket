package psu.ru.trrp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import psu.ru.trrp.pojo.UserPojo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class LoginFrmController {

    // Порты, на которых может быть развернут сервер
    private final int START_PORT = 33333;
    private final int FINISH_PORT = 33340;

    @FXML
    private Button btLogin;

    @FXML
    private TextField tfdUserName;

    @FXML
    private TextField tfdPassword;

    private Socket socket;
    private UserPojo userPojo;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    @FXML
    void initialize() {
        new Thread(() -> {
            btLogin.setDisable(true);
            List<InetAddress> addresses = new ArrayList<>();

            // Ищем все доступные IP адреса в сети
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
                                btLogin.setDisable(false);
                            }
                            System.out.println("yes!");
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

        // Подписка на событие клика по кнопке "Войти"
        btLogin.setOnAction(event -> {
            try {
                oos.writeObject(new UserPojo(null, tfdUserName.getText(), tfdPassword.getText(), ""));
                this.userPojo = (UserPojo) ois.readObject();
                if (this.userPojo != null) {
                    openMainForm();
                }
                System.out.println("yes");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    // Метод запуска основной формы
    private void openMainForm() {
        Parent root = null;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainFrm.fxml"));
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage mainFrm = new Stage();
        mainFrm.initModality(Modality.APPLICATION_MODAL);
        mainFrm.setScene(new Scene(root, 1240, 650));

        loader.<MainFrmController>getController().initialize(socket, userPojo, ois, oos);
        close();
        mainFrm.show();
    }

    // Метод закрытия формы
    private void close() {
        btLogin.getScene().getWindow().hide();
    }
}
