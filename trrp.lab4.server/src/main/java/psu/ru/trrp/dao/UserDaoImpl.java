package psu.ru.trrp.dao;

import com.google.gson.Gson;
import com.microsoft.azure.documentdb.*;
import psu.ru.trrp.dao.interfaces.UserDao;
import psu.ru.trrp.factory.DocumentClientFactory;
import psu.ru.trrp.pojo.UserPojo;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;

// Ревлизация интерфейса работы с Космос БД с коллекцией пользователей
public class UserDaoImpl implements UserDao {

    private static final String DATABASE_ID = "Chat";
    private static final String COLLECTION_ID = "Users";

    private static Database databaseCache;
    private static DocumentClient documentClient = DocumentClientFactory.getDocumentClient();
    private static DocumentCollection collectionCache;

    private static DocumentCollection getUserCollection() {
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

    private static Gson gson = new Gson();

    @Override
    public void addUser(UserPojo userPojo) {
        Document userItemDocument = new Document(gson.toJson(userPojo));
        try {
            documentClient.createDocument(
                    getUserCollection().getSelfLink(), userItemDocument, null,
                    false);
        } catch (DocumentClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public UserPojo getUser(String userName, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        List<Document> documentList = documentClient
                .queryDocuments(getUserCollection().getSelfLink(),
                        "SELECT * FROM root r WHERE r.userName = '" + userName + "'",
                        DocumentClientFactory.getQueryOptions())
                .getQueryIterable().toList();
        if (documentList.size() == 0) {
            return null;
        } else {
            UserPojo user = gson.fromJson(documentList.get(0).toString(), UserPojo.class);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(user.getSalt()), 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] testHash = new byte[0];
            try {
                testHash = factory.generateSecret(spec).getEncoded();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }

            int diff = Base64.getDecoder().decode(user.getHash()).length ^ testHash.length;
            for (int i = 0; i < Base64.getDecoder().decode(user.getHash()).length && i < testHash.length; i++) {
                diff |= Base64.getDecoder().decode(user.getHash())[i] ^ testHash[i];
            }
            return diff == 0 ? user : null;
        }
    }
}
