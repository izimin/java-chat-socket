package psu.ru.trrp.dao;

import com.google.gson.Gson;
import com.microsoft.azure.documentdb.*;
import psu.ru.trrp.dao.interfaces.MessageDao;
import psu.ru.trrp.factory.DocumentClientFactory;
import psu.ru.trrp.pojo.MessagePojo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Ревлизация интерфейса работы с Космос БД с коллекцией сообщений
public class MessageDaoImpl implements MessageDao {
    private static final String DATABASE_ID = "Chat";
    private static final String COLLECTION_ID = "Messages";

    private static Database databaseCache;
    private static DocumentClient documentClient = DocumentClientFactory.getDocumentClient();
    private static DocumentCollection collectionCache;

    private static Gson gson = new Gson();

    @Override
    public List<MessagePojo> getListMessages() {
        while (documentClient == null) {
            documentClient = DocumentClientFactory.getDocumentClient();
            try {
                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        List<Document> documentList = documentClient
                .queryDocuments(getMessageCollection().getSelfLink(),
                        "SELECT * FROM root r", DocumentClientFactory.getQueryOptions())
                .getQueryIterable().toList();
        Gson gson = new Gson();
        List<MessagePojo> messageList = new ArrayList<>();
        for (Document document : documentList) {
            messageList.add(
                    new MessagePojo(
                            UUID.fromString(document.get("id").toString()),
                            document.get("text").toString(),
                            LocalDateTime.parse((CharSequence) document.get("sentDate")),
                            UUID.fromString(document.get("userId").toString()),
                            document.get("userName").toString()
                    )
            );
        }
        return messageList;
    }

    @Override
    public void addMessage(MessagePojo message) {
        while (documentClient == null) {
            documentClient = DocumentClientFactory.getDocumentClient();
            try {
                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Document messageItemDocument = new Document(gson.toJson(message));
        messageItemDocument.set("sentDate", message.getSentDate().toString());
        try {
            documentClient.createDocument(
                    getMessageCollection().getSelfLink(), messageItemDocument, null,
                    false);
        } catch (DocumentClientException e) {
            e.printStackTrace();
        }
    }

    // Получение коллекции сообщений
    private static DocumentCollection getMessageCollection() {
        if (collectionCache == null) {
            List<DocumentCollection> collectionList = documentClient
                    .queryCollections(
                            getChatDatabase().getSelfLink(),
                            "SELECT * FROM root r WHERE r.id='" + COLLECTION_ID
                                    + "'", DocumentClientFactory.getQueryOptions()).getQueryIterable().toList();

            if (collectionList.size() > 0) {
                collectionCache = collectionList.get(0);
            } else {
                try {
                    DocumentCollection collectionDefinition = new DocumentCollection();
                    collectionDefinition.setId(COLLECTION_ID);

                    collectionCache = documentClient
                            .createCollection(getChatDatabase().getSelfLink(), collectionDefinition, null)
                            .getResource();
                } catch (DocumentClientException e) {
                    e.printStackTrace();
                }
            }
        }

        return collectionCache;
    }

    // Получение экземпляра БД
    private static Database getChatDatabase() {
        if (databaseCache == null) {
            List<Database> databaseList = documentClient
                    .queryDatabases("SELECT * FROM root r WHERE r.id='" + DATABASE_ID + "'", null)
                    .getQueryIterable()
                    .toList();

            if (databaseList.size() > 0) {
                databaseCache = databaseList.get(0);
            } else {
                try {
                    Database databaseDefinition = new Database();
                    databaseDefinition.setId(DATABASE_ID);
                    databaseCache = documentClient
                            .createDatabase(databaseDefinition, null)
                            .getResource();

                } catch (DocumentClientException e) {
                    e.printStackTrace();
                }
            }
        }
        return databaseCache;
    }
}
