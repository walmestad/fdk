package no.difi.dcat.datastore;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Map;

/**
 * Represents connection to Elasticsearch instance
 */
public class Elasticsearch implements AutoCloseable {

    private static final String DCAT_INDEX_SETUP_FILENAME = "index_setup.json";
    private static final String CLUSTER_NAME = "cluster.name";
    private final Logger logger = LoggerFactory.getLogger(Elasticsearch.class);

    private Client client;

    /**
     * Creates connection to a particular elasticsearch cluster
     *
     * @param host        IP address or hostname where cluster can be reached
     * @param port        Port number for connection to cluster. Usually 9300
     * @param clusterName Name of cluster. Default is "elasticsearch"
     */
    public Elasticsearch(String host, int port, String clusterName) {
        logger.debug("Attempt to connect to Elasticsearch client: " + host + ":" + port + " cluster: " + clusterName);
        this.client = returnElasticsearchTransportClient(host, port, clusterName);
        logger.debug("transportclient success ...? " + this.client);
    }


    /**
     * Set elastic search transport client
     *
     * @param client An elasticsearch transport client
     */
    public Elasticsearch(Client client) {
        this.client = client;
    }


    /**
     * Creates elasticsearch transport client and returns it to caller
     *
     * @param host        IP address or hostname where cluster can be reached
     * @param port        Port number for connection to cluster. Usually 9300
     * @param clusterName Name of cluster. Default is "elasticsearch"
     * @return elasticsearch transport client bound to hostname, port and cluster specified in input parameters
     */
    public Client returnElasticsearchTransportClient(String host, int port, String clusterName) {
        Client client = null;
        try {
            logger.debug("Connect to elasticsearch: " + host + " : " + port + " cluster: " + clusterName);

            InetAddress inetaddress = InetAddress.getByName(host);
            logger.debug("ES inetddress: " + inetaddress.toString());
            InetSocketTransportAddress address = new InetSocketTransportAddress(inetaddress, port);
            logger.debug("ES address: " + address.toString());
            Settings settings = Settings.builder()
                    .put(CLUSTER_NAME, clusterName).build();

            client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(address);

            logger.debug("Client returns! " + address.toString());

        } catch (UnknownHostException e) {
            logger.error(e.toString());
        }

        logger.debug("transportclient: " + client);
        return client;

    }

    /**
     * Check to see if Elasticsearch cluster is answering
     *
     * @return True if cluster is running
     */
    public boolean isElasticsearchRunning() {
        return elasticsearchStatus() != null;
    }


    /**
     * Return status of Elasticsearch cluster. status can be either GREEN, YELLOW or RED.
     *
     * @return one value of ClusterHealthStatus enumeration: GREEN, YELLOW or RED
     */
    public ClusterHealthStatus elasticsearchStatus() {
        return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
    }


    /**
     * Checks if a particular document exists in elasticsearch index
     *
     * @param index name of index. Example "dcat"
     * @param type  Docuyment type. Example: "dataset"
     * @param id    ID of document
     * @return True if document exists
     */
    public boolean documentExists(String index, String type, String id) {
        return client.prepareGet(index, type, id).execute().actionGet().isExists();
    }


    /**
     * Checks if a particular index exists on elasticsearch cluster
     *
     * @param index name of index. Example "dcat"
     * @return True if index exists
     */
    public boolean indexExists(String index) {
        return client.admin().indices().prepareExists(index).execute().actionGet().isExists();
    }

    /**
     * Checks if a particular type exists on elasticsearch cluster
     *
     * @param type name of type.
     * @return True if index exists
     */
    public boolean typeExists(String type) {
        return client.admin().indices().prepareTypesExists(type).execute().actionGet().isExists();
    }

    /**
     * Creates new dcat and theme indexes on Elasticsearch cluster
     * The indexes are set up correct indexing fields and language stemming
     * Configuration of the indexes is specified in DCAT_INDEX_SETUP_FILEMAME
     *
     * @param index Name of index to be created
     */
    public void createIndex(String index) {
        //Set mapping for correct language stemming and indexing
        ClassLoader classLoader = getClass().getClassLoader();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:" + DCAT_INDEX_SETUP_FILENAME);

            for (Resource r : resources) {

                InputStream is = r.getInputStream();

                String mappingJson = IOUtils.toString(is, "UTF-8");


                logger.debug("[createIndex] mappingJson prepared: " + mappingJson);

                client.admin().indices().prepareCreate(index).execute().actionGet();
                logger.debug("[createIndex] before preparePutMapping");
                client.admin().indices().preparePutMapping(index).setType("dataset").setSource(mappingJson).execute().actionGet();
                logger.debug("[createIndex] after preparePutMapping");
                client.admin().cluster().prepareHealth(index).setWaitForYellowStatus().execute().actionGet();
                logger.debug("[createIndex] after prepareHealth");
            }
        } catch (IOException e) {
            logger.error("Unable to create index for Elasticsearch " + e.getMessage());
        }
    }


    /**
     * Puts a new Json document into specified index
     *
     * @param index      Index where json document is stored
     * @param type       Document type to be stored. Example: "dataset"
     * @param id         ID of document to be stored
     * @param jsonObject The Json object to representing the object to be stored
     * @return True if successful
     */
    public boolean indexDocument(String index, String type, String id, JsonObject jsonObject) {
        IndexResponse rsp = client.prepareIndex(index, type, id).setSource(jsonObject).execute().actionGet();
        return rsp.isCreated();
    }


    /**
     * Puts a new Json array into index as a document
     *
     * @param index     Index where json array is stored
     * @param type      Document type to be stored. Example: "dataset"
     * @param id        ID of document to be stored
     * @param jsonArray Json array to be stored
     * @return True if successful
     */
    public boolean indexDocument(String index, String type, String id, JsonArray jsonArray) {
        IndexResponse rsp = client.prepareIndex(index, type, id).setSource(jsonArray).execute().actionGet();
        return rsp.isCreated();
    }


    /**
     * Puts a new string object into index as a document
     *
     * @param index  Index where the string is to be stored
     * @param type   Document type to be stored. Example: "dataset"
     * @param id     ID of document to be stored
     * @param string String representing the contents of the document that is to be stored
     * @return True if successful
     */
    public boolean indexDocument(String index, String type, String id, String string) {
        IndexResponse rsp = client.prepareIndex(index, type, id).setSource(string).execute().actionGet();
        return rsp.isCreated();
    }


    /**
     * Puts a new object map into index as document
     *
     * @param index Index where the oject map is to be stored
     * @param type  Document type to be stored. Example: "dataset"
     * @param id    ID of document to be stored
     * @param map   Object map to be stored
     * @return True if successful
     */
    public boolean indexDocument(String index, String type, String id, Map<String, Object> map) {
        IndexResponse rsp = client.prepareIndex(index, type, id).setSource(map).execute().actionGet();
        return rsp.isCreated();
    }


    /**
     * Delete document from Elasticsearch index
     *
     * @param index Name of index where the document to be deleted is stored
     * @param type  Type of document to be deleted
     * @param id    ID of document to be deleted
     * @return True if successful
     */
    public boolean deleteDocument(String index, String type, String id) {
        DeleteResponse rsp = client.prepareDelete(index, type, id).execute().actionGet();
        return !rsp.isFound();
    }

    /**
     * Delete all documents from Elasticsearch index and type
     *
     * @param index Name of index where the type to be deleted is stored
     * @param type  Type of document to be deleted
     * @param size  Maximum number of document to be deleted
     * @return True if documents has been successfully deleted.
     */
    public boolean deleteAllDocumentsInType(String index, String type, int size) {

        if(!indexExists(index)) {
            return false;
        }

        SearchResponse sr = this.getClient().prepareSearch(index).setTypes(type).
                setQuery(QueryBuilders.matchAllQuery()).setSize(size).get();

        if (sr.getHits().getHits().length == 0) {
            return false;
        }

        for (SearchHit s : sr.getHits().getHits()) {
            this.getClient().prepareDelete(index, type, s.getId()).get();
        }
        return true;
    }


    /**
     * Close connection to Elasticsearch cluster
     */
    public void close() {
        client.close();
    }

    /**
     * Get Elasticsearc transport client instance
     *
     * @return Elasticsearch transport client
     */
    public Client getClient() {
        return client;
    }
}
