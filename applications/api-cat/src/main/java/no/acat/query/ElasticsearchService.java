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

    @Value("${application.elasticsearchHosts}")
    private String elasticsearchHosts;

    @Value("${application.clusterName}")
    private String clusterName;

    @PostConstruct
    void validate(){
        assert elasticsearchHosts != null;
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
        logger.debug("elasticsearch: " + elasticsearchHosts);
        if (elasticsearch == null) {
            if (elasticsearchHosts == null) {
                logger.error("Configuration property application.elasticsearchHost is not initialized. Unable to connect to Elasticsearch");
            }

            elasticsearch = new Elasticsearch(elasticsearchHosts, clusterName);
        }
    }

}
