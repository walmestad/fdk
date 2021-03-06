package no.fdk.searchapi.controller;

import no.fdk.searchapi.service.ElasticsearchService;
import no.fdk.test.testcategories.UnitTest;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Class for testing getDatasetByIdHandler Rest-API in DatasetsQueryService.
 */
@RunWith(MockitoJUnitRunner.class)
@Category(UnitTest.class)
public class DatasetsSearchControllerTest {

    DatasetsSearchController sqs;
    Client client;

    @Before
    public void setUp() {
        client = mock(Client.class);
        populateMock();
        ElasticsearchService elasticsearchServiceMock = mock(ElasticsearchService.class);
        when(elasticsearchServiceMock.getClient()).thenReturn(client);
        sqs = new DatasetsSearchController(elasticsearchServiceMock);
    }

    /**
     * Valid call, with sortdirection set.
     */
    @Test
    public void testValidWithSortdirection() {
        ResponseEntity<String> actual = sqs.search("query", "", "", "", "", 0, "nb", "modified", "asc", "", "", "", "", "", "", PageRequest.of(0, 10));

        verify(client.prepareSearch("dcat")
            .setTypes("dataset")
            .setQuery(any(QueryBuilder.class))
            .setFrom(1).setSize(10))
            .addSort(SortBuilders.fieldSort("harvest.firstHarvested").order(SortOrder.ASC).missing("_last"));
        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());

        sqs.search("query", "", "", "", "", 0, "nb", "title.nb", "asc", "", "", "", "", "", "", PageRequest.of(0, 10));
    }

    /**
     * Valid call, check default sort direction.
     */
    @Test
    public void testValidWithDefaultSortdirection() {
        ResponseEntity<String> actual = sqs.search("query", "", "", "", "", 0, "nb", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));

        verify(client.prepareSearch("dcat").setTypes("dataset").setQuery(any(QueryBuilder.class)).setFrom(1)).setSize(10);
        verify(client.prepareSearch("dcat").setTypes("dataset").setQuery(any(QueryBuilder.class)).setFrom(1).setSize(10), never()).addSort("", SortOrder.ASC);
        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * Valid call, with tema set.
     */
    @Test
    public void testValidWithTema() {
        ResponseEntity<String> actual = sqs.search("query", "", "GOVE", "", "", 0, "nb", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));

        verify(client.prepareSearch("dcat").setTypes("dataset").setQuery(any(QueryBuilder.class)).setFrom(1)).setSize(10);
        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());

        sqs.search("", "", "Ukjent", "", "", 0, "nb", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));
    }

    /**
     * Inputparameter validation. Over 100 size shall throw http-error bad request.
     */
    @Test
    public void return200IfSizeIsLargerThan100() {
        ResponseEntity<String> actual = sqs.search("", "", "", "", "", 0, "nb", "", "", "", "", "", "", "", "", PageRequest.of(1, 101));

        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void checkSortfields() {
        sqs.search("", "", "", "", "", 0, "en", "modified", "", "", "", "", "", "", "", PageRequest.of(0, 10));
    }

    @Test
    public void checkAccessRights() {
        sqs.search("", "", "", "Ukjent", "", 0, "", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "OPEN", "", 0, "", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));
    }

    @Test
    public void checkOrgpath() {
        sqs.search("", "", "", "", "/ANNET", 0, "", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));
    }


    @Test
    public void checkTitle() {
        sqs.search("", "TITLE", "", "", "/ANNET", 0, "", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));
    }

    @Test
    public void testTitle() {
        ResponseEntity<String> actual = sqs.search("", "TITLE", "", "", "/ANNET", 0, "nb", "", "", "", "", "", "", "", "", PageRequest.of(0, 10));

        verify(client.prepareSearch("dcat").setTypes("dataset").setQuery(any(QueryBuilder.class)).setFrom(1)).setSize(10);
        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void checkProvenance() {

        sqs.search("", "", "", "", "", 0, "", "", "", "NASJONAL", "", "", "", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "", "", 0, "", "", "", "VEDTAK", "", "", "", "", "", PageRequest.of(0, 10));
    }

    @Test
    public void checkSpatial() {

        sqs.search("", "", "", "", "", 0, "", "", "", "", "Ukjent", "", "", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "", "", 0, "", "", "", "", "Norge", "", "", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "", "", 0, "", "", "", "", "Oslo", "", "", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "", "", 0, "", "", "", "", "barbara", "", "", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "", "", 0, "", "", "", "", "http://tulletse", "", "", "", "", PageRequest.of(0, 10));
    }

    @Test
    public void checkMultipleSpatialLabels() {
        sqs.search("", "", "", "", "", 0, "", "", "", "", "Ukjent,Oslo Fylke", "", "", "", "", PageRequest.of(0, 10));
    }

    @Test
    public void checkOpendata() {
        sqs.search("", "", "", "", "", 0, "", "", "", "", "", "true", "", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "", "", 0, "", "", "", "", "", "false", "", "", "", PageRequest.of(0, 10));
    }

    @Test
    public void checkCatalog() {
        sqs.search("", "", "", "", "", 0, "", "", "", "", "", "", "http://catalog.url", "", "", PageRequest.of(0, 10));
        sqs.search("", "", "", "", "", 0, "", "", "", "", "", "", "Katalog for Brønnøysundregistrene", "", "", PageRequest.of(0, 10));
    }

    private void populateMock() {
        SearchResponse response = mock(SearchResponse.class);

        ListenableActionFuture<SearchResponse> action = mock(ListenableActionFuture.class);
        when(action.actionGet()).thenReturn(response);

        SearchRequestBuilder builder = mock(SearchRequestBuilder.class);
        when(builder.setTypes("dataset")).thenReturn(builder);
        when(builder.setQuery((QueryBuilder) any())).thenReturn(builder);
        when(builder.setFrom(anyInt())).thenReturn(builder);
        when(builder.setSize(anyInt())).thenReturn(builder);
        when(builder.addSort(any())).thenReturn(builder);
        when(builder.addAggregation(any(AbstractAggregationBuilder.class))).thenReturn(builder);
        when(builder.execute()).thenReturn(action);

        when(client.prepareSearch("dcat")).thenReturn(builder);
    }

}
