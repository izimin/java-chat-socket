package psu.ru.trrp.pojo;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.Getter;

import java.time.LocalDateTime;

/*
 * Класс компонента, который представлет собой сообщение на формочке
 * */

@Getter
public class ChatItem {
    HBox myMsgBox = new HBox(10);

    public ChatItem(MessagePojo messagePojo, boolean isToMe, boolean isFromBot) {
        HBox msgBox = new HBox(10);
        msgBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        msgBox.setAlignment(Pos.BOTTOM_LEFT);
        msgBox.setStyle("-fx-background-color: " + (isFromBot ? "#289D8F;" : isToMe ? "#182533;" : "#2B5278; ") +
                "-fx-background-radius: 10 10 10 10;" +
                "-fx-padding: 10;");
        Text name = new Text(messagePojo.getUserName());
        name.setFill(Paint.valueOf("#FAA357"));
        name.setFont(Font.font("", FontWeight.BOLD, 16));

        Text textMsg = new Text(messagePojo.getText());
        textMsg.setFill(Paint.valueOf("White"));
        textMsg.setFont(Font.font(16));

        LocalDateTime ldt = messagePojo.getSentDate();
        Text timeData = new Text(String.format("%d:%d", ldt.getHour(), ldt.getMinute()));
        timeData.setFill(Paint.valueOf("#B4BAC4"));
        timeData.setFont(Font.font(17));
        timeData.setStyle("-fx-padding: 10");
        msgBox.getChildren().addAll(new VBox(name, textMsg), timeData);

        myMsgBox.setAlignment(isToMe || isFromBot ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
        myMsgBox.getChildren().add(msgBox);
    }
}