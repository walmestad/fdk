package no.dcat.portal.query;

import no.dcat.datastore.Elasticsearch;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.annotation.PostConstruct;


public class ElasticsearchService {
    private static Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    @Value("${application.elasticsearchHosts}")
    private String elasticsearchHosts;

    @Value("${application.clusterName}")
    private String clusterName;

    @PostConstruct
    void validate(){
        assert elasticsearchHosts != null;
        assert clusterName != null;
    }


    private Elasticsearch elasticsearch;

    public Client getClient() {
        if (elasticsearch == null) {
            initializeElasticsearchTransportClient();
        }
        return elasticsearch==null ? null : elasticsearch.getClient();
    }

    void setClient(Client client) {
        elasticsearch = new Elasticsearch(client);
    }

    public ResponseEntity<String> initializeElasticsearchTransportClient() {
        String jsonError = "{\"error\": \"Query service is not properly initialized. Unable to connect to database (ElasticSearch)\"}";

        logger.debug("elasticsearch: " + elasticsearchHosts);
        if (elasticsearch == null) {
            if (elasticsearchHosts == null) {
                logger.error("Configuration property application.elasticsearchHosts is not initialized. Unable to connect to Elasticsearch");
                return new ResponseEntity<>(jsonError, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            elasticsearch = new Elasticsearch(elasticsearchHosts, clusterName);
        }
        return null;
    }
}
