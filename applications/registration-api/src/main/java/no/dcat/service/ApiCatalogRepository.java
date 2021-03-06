package no.dcat.service;

import no.dcat.model.ApiCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface ApiCatalogRepository extends ElasticsearchRepository<ApiCatalog, String> {

    //todo there is something wrong with the autogenerated query. (0 items returned)
    // With this annotation, the query works
    @Query("{\"term\":{\"orgNo\":\"?0\"}}")
    Optional<ApiCatalog> findByOrgNo(String orgNo);

    Page<ApiCatalog> findAll(); //Find all returns something that fails to convert to list but does convert well to Page (use .getContent() to get to the list)
    //See: https://stackoverflow.com/questions/46150275/get-all-documents-from-an-index-using-spring-data-elasticsearch
}
