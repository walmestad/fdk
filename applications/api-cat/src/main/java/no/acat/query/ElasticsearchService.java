package no.acat.query;

import no.dcat.datastore.Elasticsearch;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ElasticsearchService {
    private static Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    @Value("${elastic.clusterNodes}")
    private String clusterNodes;

    @Value("${elastic.clusterName}")
    private String clusterName;

    @PostConstruct
    void validate(){
        assert clusterNodes != null;
        assert clusterName != null;

        initializeElasticsearchTransportClient();
    }


    private Elasticsearch elasticsearch;

    public Client getClient() {
        if (elasticsearch == null) {
            initializeElasticsearchTransportClient();
        }
        return elasticsearch==null ? null : elasticsearch.getClient();
    }

    private void initializeElasticsearchTransportClient() {
        logger.debug("elasticsearch: " + clusterNodes);
        if (elasticsearch == null) {
            if (clusterNodes == null) {
                logger.error("Configuration property elastic.clusterNodes is not initialized. Unable to connect to Elasticsearch");
            }

            elasticsearch = new Elasticsearch(clusterNodes, clusterName);
        }
    }

}
