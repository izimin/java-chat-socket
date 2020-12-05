package psu.ru.trrp.factory;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.FeedOptions;
import psu.ru.trrp.config.AccountCredentials;

public class DocumentClientFactory {
    // Клиент Cosmos DB
    private static DocumentClient documentClient = new DocumentClient(
            AccountCredentials.HOST,
            AccountCredentials.MASTER_KEY,
            ConnectionPolicy.GetDefault(),
            ConsistencyLevel.Session
    );

    private static FeedOptions queryOptions = new FeedOptions();

    public static FeedOptions getQueryOptions() {
        queryOptions.setEnableCrossPartitionQuery(true);
        return queryOptions;
    }

    public static DocumentClient getDocumentClient() {
        return documentClient;
    }
}
