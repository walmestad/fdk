package no.dcat.bddtest.elasticsearch.client;


import no.dcat.datastore.Elasticsearch;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.index.IndexNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for deleting index in Elasticsearch.
 */
public class DeleteIndex {
    private final Logger logger = LoggerFactory.getLogger(DeleteIndex.class);

    private final String hosts;
    private final String clustername = "elasticsearch";

    public DeleteIndex(String hosts) {
        this.hosts = hosts;
    }

    public void deleteIndex(String index) {
        try (Elasticsearch elasticsearch = new Elasticsearch(hosts, clustername)) {
            logger.info("Deleting indexing {}", index);
            deleteIndexInElasticsearch(elasticsearch, index);
        } catch (Exception e) {
            logger.error("Exception occurred while deleting index: {}", e.getMessage());
            throw e;
        }
    }

    private void deleteIndexInElasticsearch(Elasticsearch elasticsearch, String index) {
        try {
            elasticsearch.getClient().admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
            elasticsearch.getClient().admin().indices().prepareRefresh().get();

        } catch (IndexNotFoundException e) {
            logger.info("Index not found: {}", index);
        }
    }
}
